#!/bin/bash

# Teste de Concorrência para Compra de Cartas

# --- Configurações ---
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$(dirname "$SCRIPT_DIR")")"
DOCKER_DIR="$PROJECT_ROOT/docker"

# --- Funções ---

# Função para limpar o ambiente
cleanup() {
    echo "[CLEANUP] Parando e removendo contêineres..."
    docker compose -f "$DOCKER_DIR/docker-compose.yml" down --remove-orphans -v 2>/dev/null || true
    # Deletar o log antigo para garantir que estamos lendo apenas o resultado deste teste
    rm -f "$PROJECT_ROOT/all_server_logs.log"
}

# Função para obter valor do Redis e extrair apenas o número
get_redis_value() {
    local key="$1"
    local field="$2"
    # Executar o comando e limpar a saída para obter apenas o valor numérico
    local result=$(docker compose -f "$DOCKER_DIR/docker-compose.yml" exec redis-master redis-cli HGET "$key" "$field" 2>/dev/null | sed 's/^ *(integer) *//; s/^ *(nil)$//')
    # Remover linhas vazias e espaços extras
    echo "$result" | tr -d '\r\n' | xargs echo
}

# --- Execução ---

# Garantir que a limpeza seja executada ao sair
trap cleanup EXIT

# 1. Limpeza Inicial
echo "[INIT] Iniciando limpeza do ambiente..."
cleanup

# 2. Setup do Ambiente
echo "[SETUP] Iniciando ambiente com Docker Compose..."
docker compose -f "$DOCKER_DIR/docker-compose.yml" up -d --build nginx-gateway server-1 server-2 server-3 server-4 redis-master postgres redis-sentinel-1 redis-sentinel-2 redis-sentinel-3 redis-slave

echo "[SETUP] Aguardando 90s para os serviços estabilizarem (nginx-gateway tem healthcheck mais longo)..."
sleep 90

# Verificar se os serviços estão rodando
RUNNING_SERVICES=$(docker compose -f "$DOCKER_DIR/docker-compose.yml" ps --format "table {{.Status}}" | grep -c "Up\|running" || true)
echo "[SETUP] Serviços em execução: $RUNNING_SERVICES"
if [ "$RUNNING_SERVICES" -lt 6 ]; then
    echo "[ERRO] Nem todos os serviços foram iniciados corretamente ($RUNNING_SERVICES/6). Abortando."
    docker compose -f "$DOCKER_DIR/docker-compose.yml" ps
    exit 1
fi

# Aguardar adicionalmente até que o nginx-gateway esteja realmente pronto para receber conexões
echo "[SETUP] Aguardando o nginx-gateway estar pronto para receber conexões..."
for i in {1..30}; do
    if curl -f http://localhost:8080/actuator/health >/dev/null 2>&1; then
        echo "[SETUP] Nginx-gateway está pronto para receber conexões."
        break
    else
        echo "[SETUP] Nginx-gateway ainda não está pronto, tentando novamente em 5s... ($i/30)"
        sleep 5
    fi
    if [ $i -eq 30 ]; then
        echo "[ERRO] Nginx-gateway não ficou pronto após 150s. Abortando."
        exit 1
    fi
done

# Configurar o estoque inicial no Redis
# Usaremos a carta "legendary-1" que é usada pelo comando LEGENDARY
echo "[SETUP] Configurando estoque inicial da carta 'legendary-1' para 10 no Redis..."
docker compose -f "$DOCKER_DIR/docker-compose.yml" exec redis-master redis-cli HSET "card:stock" "legendary-1" 10

# Verificar se o estoque foi configurado (with proper value extraction)
INITIAL_STOCK=$(get_redis_value "card:stock" "legendary-1")
echo "[DEBUG] Initial stock value: '$INITIAL_STOCK'"

if [ -z "$INITIAL_STOCK" ] || [ "$INITIAL_STOCK" = "(nil)" ] || [ "$INITIAL_STOCK" != "10" ]; then
    echo "[ERRO] Falha ao configurar o estoque inicial no Redis. Valor obtido: '$INITIAL_STOCK'. Abortando."
    exit 1
fi
echo "[SETUP] Estoque inicial de 'legendary-1' é $INITIAL_STOCK."

# 3. Execução do Teste de Estresse

# Compilar o cliente de estresse
echo "[BUILD] Compilando o cliente de estresse..."
cd "$PROJECT_ROOT/dueling-client"
if ! mvn clean install -DskipTests -q; then
    echo "[ERRO] Falha ao compilar o cliente de estresse. Abortando."
    exit 1
fi

echo "[TEST] Iniciando 20 clientes simultâneamente para teste de concorrência..."

# Criar o diretório de logs se não existir
mkdir -p "$PROJECT_ROOT/test_logs"

# Executar 20 instâncias do cliente de teste em paralelo
for i in {1..20}; do
    echo "[TEST] Iniciando cliente $i..."
    # Cada cliente terá um nome de usuário único
    # Redirecionar a saída para arquivos de log individuais para debug
    mvn exec:java -Dexec.mainClass="client.StockStressClient" -Dexec.args="concurrent_user_$i password" > "$PROJECT_ROOT/test_logs/client_$i.log" 2>&1 &
done

# Aguardar um PID para verificar se os clientes realmente iniciaram
CLIENT_PIDS=$(jobs -p)
echo "[TEST] Clientes iniciados com PIDs: $CLIENT_PIDS"

echo "[TEST] Aguardando 90s para que todas as tentativas de compra concorrentes ocorram..."
sleep 90

# Aguardar todos os processos em segundo plano terminarem
wait

# 4. Verificação (Assert)
echo "[ASSERT] Coletando logs de todos os servidores..."
LOG_FILE="$PROJECT_ROOT/all_server_logs.log"
docker compose -f "$DOCKER_DIR/docker-compose.yml" logs server-1 server-2 server-3 server-4 > "$LOG_FILE" 2>&1

SUCCESS_COUNT=$(grep -c "SUCCESS:Pack purchased" "$LOG_FILE")
SUCCESS_COUNT=${SUCCESS_COUNT:-0}
echo "[ASSERT] Número de compras com sucesso encontradas nos logs: $SUCCESS_COUNT"

OUT_OF_STOCK_COUNT=$(grep -c "OUT_OF_STOCK\|Probably out of stock\|Insufficient stock\|Stock not available" "$LOG_FILE")
OUT_OF_STOCK_COUNT=${OUT_OF_STOCK_COUNT:-0}
echo "[ASSERT] Número de falhas por estoque esgotado encontradas nos logs: $OUT_OF_STOCK_COUNT"

FINAL_STOCK=$(get_redis_value "card:stock" "legendary-1")
# Se a chave não existir, o Redis retorna (nil). O cliente CLI pode retornar uma string vazia ou um erro.
# Vamos tratar nil, string vazia e "0" como 0.
if [ -z "$FINAL_STOCK" ] || [ "$FINAL_STOCK" == "(nil)" ] || [ "$FINAL_STOCK" == "" ]; then
    FINAL_STOCK=0
fi
echo "[ASSERT] Estoque final no Redis: $FINAL_STOCK"

# 5. Validar Resultados
echo "[ASSERT] Validando os resultados..."
echo "[ASSERT] Esperado: 10 sucessos, 10+ falhas, estoque final 0"
echo "[ASSERT] Obtido: $SUCCESS_COUNT sucessos, $OUT_OF_STOCK_COUNT falhas, estoque final $FINAL_STOCK"

SUCCESS_EXPECTED=10
if [ "$SUCCESS_COUNT" -eq "$SUCCESS_EXPECTED" ] && [ "$FINAL_STOCK" -eq 0 ]; then
    echo -e "\n[SUCESSO] O teste de concorrência passou."
    echo "O lock distribuído funcionou corretamente: $SUCCESS_COUNT compras tiveram sucesso e o estoque final é $FINAL_STOCK."
    # A limpeza final será executada pelo trap
    exit 0
else
    echo -e "\n[FALHA] O teste de concorrência falhou."
    echo "Resultados esperados: $SUCCESS_EXPECTED sucessos, estoque final 0."
    echo "Resultados obtidos: $SUCCESS_COUNT sucessos, estoque final $FINAL_STOCK."
    
    # Mostrar logs de exemplo para debug
    echo -e "\n[DEBUG] Últimas 20 linhas dos logs dos servidores:"
    tail -n 20 "$LOG_FILE"
    
    # Mostrar logs dos clientes para debug
    echo -e "\n[DEBUG] Logs de alguns clientes (primeiros 5):"
    for i in {1..5}; do
        if [ -f "$PROJECT_ROOT/test_logs/client_$i.log" ]; then
            echo "--- Client $i log ---"
            head -n 10 "$PROJECT_ROOT/test_logs/client_$i.log"
            echo "... (truncated)"
        fi
    done
    
    # A limpeza final será executada pelo trap
    exit 1
fi
