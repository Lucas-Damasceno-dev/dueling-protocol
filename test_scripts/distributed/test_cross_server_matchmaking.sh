#!/usr/bin/env bash

# Para o script se qualquer comando falhar
set -e

echo "======================================================="
echo ">>> INICIANDO TESTE DE PAREAMENTO CROSS-SERVIDOR"
echo "======================================================="

# Define o diretório raiz do projeto
# (Este script deve estar em test_scripts/distributed/)
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
PROJECT_ROOT="$SCRIPT_DIR/../.."
DOCKER_COMPOSE_FILE="$PROJECT_ROOT/docker/docker-compose.yml"
ENV_FILE_S1="$PROJECT_ROOT/.env.s1"
ENV_FILE_S2="$PROJECT_ROOT/.env.s2"

# Função de limpeza para garantir que os containers parem
cleanup() {
  echo
  echo ">>> Limpando ambiente Docker..."
  # Garante que ambos os 'composes' (mesmo que usem o mesmo file) sejam derrubados
  docker compose -f "$DOCKER_COMPOSE_FILE" --env-file "$ENV_FILE_S1" down --remove-orphans 2>/dev/null || true
  docker compose -f "$DOCKER_COMPOSE_FILE" --env-file "$ENV_FILE_S2" down --remove-orphans 2>/dev/null || true
  rm -f "$ENV_FILE_S1" "$ENV_FILE_S2"
  rm -f "$PROJECT_ROOT/server1_logs.txt" "$PROJECT_ROOT/server2_logs.txt"
  echo ">>> Limpeza concluída."
}
# Garante que a limpeza seja executada ao sair do script (sucesso ou erro)
trap cleanup EXIT

# --- 1. Build (Assumindo que já foi feito, mas garantindo) ---
echo ">>> [PASSO 1/5] Garantindo que as imagens Docker estão construídas..."
# Criar um .env temporário para o build com todas as variáveis necessárias
cat > "$PROJECT_ROOT/.env" <<EOL
BOT_MODE=autobot
BOT_SCENARIO=
# Default values for required environment variables
POSTGRES_HOST=postgres
POSTGRES_PORT=5432
POSTGRES_DB=duelingdb
POSTGRES_USER=duelinguser
POSTGRES_PASSWORD=duelingpass
GATEWAY_HOST=nginx-gateway
GATEWAY_PORT=8080
REDIS_SENTINEL_MASTER=mymaster
REDIS_SENTINEL_NODES=redis-sentinel-1:26379,redis-sentinel-2:26379,redis-sentinel-3:26379
EOL
# Usamos --env-file para evitar avisos de variáveis faltando
docker compose -f "$DOCKER_COMPOSE_FILE" --env-file "$PROJECT_ROOT/.env" build
rm -f "$PROJECT_ROOT/.env"

# --- 2. Iniciar Infraestrutura e Servidores ---
echo ">>> [PASSO 2/5] Iniciando infra (Postgres, Redis) e Servidores (server-1, server-2)..."
# Criar .env com todas as variáveis necessárias, pois só queremos subir os servidores
cat > "$ENV_FILE_S1" <<EOL
BOT_MODE=autobot
BOT_SCENARIO=
# Default values for required environment variables
POSTGRES_HOST=postgres
POSTGRES_PORT=5432
POSTGRES_DB=duelingdb
POSTGRES_USER=duelinguser
POSTGRES_PASSWORD=duelingpass
GATEWAY_HOST=nginx-gateway
GATEWAY_PORT=8080
REDIS_SENTINEL_MASTER=mymaster
REDIS_SENTINEL_NODES=redis-sentinel-1:26379,redis-sentinel-2:26379,redis-sentinel-3:26379
EOL
# Inicia a infra e os dois servidores, mas NENHUM cliente ou gateway
docker compose -f "$DOCKER_COMPOSE_FILE" --env-file "$ENV_FILE_S1" up --scale client-1=0 --scale client-2=0 --scale client-3=0 --scale client-4=0 --scale nginx-gateway=0 -d server-1 server-2 postgres redis-master redis-slave redis-sentinel-1 redis-sentinel-2 redis-sentinel-3
echo ">>> Aguardando servidores iniciarem (30 segundos)..."
sleep 30

# --- 3. Iniciar Cliente 1 (Conectado ao Server-1) ---
echo ">>> [PASSO 3/5] Iniciando Cliente 1 (conectado ao server-1)..."
# Criar .env específico para o cliente 1
cat > "$ENV_FILE_S1" <<EOL
BOT_MODE=autobot
BOT_SCENARIO=
# Conecta o cliente DIRETAMENTE ao server-1 (usando o nome do serviço docker)
GATEWAY_HOST=server-1
# Porta interna do container do servidor
GATEWAY_PORT=8080
CLIENT_USERNAME=client_s1
CLIENT_PASSWORD=password
DOCKER_ENV=true
# Default values for required environment variables
POSTGRES_HOST=postgres
POSTGRES_PORT=5432
POSTGRES_DB=duelingdb
POSTGRES_USER=duelinguser
POSTGRES_PASSWORD=duelingpass
REDIS_SENTINEL_MASTER=mymaster
REDIS_SENTINEL_NODES=redis-sentinel-1:26379,redis-sentinel-2:26379,redis-sentinel-3:26379
EOL
# Inicia APENAS o client-1, usando seu .env customizado
docker compose -f "$DOCKER_COMPOSE_FILE" --env-file "$ENV_FILE_S1" up -d client-1

# --- 4. Iniciar Cliente 2 (Conectado ao Server-2) ---
echo ">>> [PASSO 4/5] Iniciando Cliente 2 (conectado ao server-2)..."
# Criar .env específico para o cliente 2
cat > "$ENV_FILE_S2" <<EOL
BOT_MODE=autobot
BOT_SCENARIO=
# Conecta o cliente DIRETAMENTE ao server-2 (usando o nome do serviço docker)
GATEWAY_HOST=server-2
# Porta interna do container do servidor
GATEWAY_PORT=8080
CLIENT_USERNAME=client_s2
CLIENT_PASSWORD=password
DOCKER_ENV=true
# Default values for required environment variables
POSTGRES_HOST=postgres
POSTGRES_PORT=5432
POSTGRES_DB=duelingdb
POSTGRES_USER=duelinguser
POSTGRES_PASSWORD=duelingpass
REDIS_SENTINEL_MASTER=mymaster
REDIS_SENTINEL_NODES=redis-sentinel-1:26379,redis-sentinel-2:26379,redis-sentinel-3:26379
EOL
# Inicia APENAS o client-2, usando seu .env customizado
docker compose -f "$DOCKER_COMPOSE_FILE" --env-file "$ENV_FILE_S2" up -d client-2

# --- 5. Verificar Resultados ---
echo ">>> [PASSO 5/5] Clientes iniciados. Aguardando 30 segundos para o pareamento..."
sleep 30

echo ">>> Verificando logs do server-1 (deve tentar pareamento remoto)..."
# O server-1 (onde o client-1 se conectou) deve tentar buscar um parceiro remoto
docker compose -f "$DOCKER_COMPOSE_FILE" --env-file "$ENV_FILE_S1" logs server-1 > "$PROJECT_ROOT/server1_logs.txt"
# Procuramos pela lógica no GameFacade
if grep -i "Found remote partner" "$PROJECT_ROOT/server1_logs.txt"; then
  echo -e "\e[32m>>> SUCESSO: Server-1 encontrou um parceiro remoto.\e[0m"
elif grep -i "findAndLockPartner" "$PROJECT_ROOT/server1_logs.txt"; then
  echo -e "\e[32m>>> SUCESSO: Server-1 buscou um parceiro (findAndLockPartner).\e[0m"
else
  echo -e "\e[33m>>> AVISO: Não foi encontrado log explícito de busca remota no server-1.\e[0m"
fi

echo ">>> Verificando logs do server-2 (deve receber a solicitação)..."
# O server-2 (onde o client-2 se conectou) deve receber a chamada da API
docker compose -f "$DOCKER_COMPOSE_FILE" --env-file "$ENV_FILE_S2" logs server-2 > "$PROJECT_ROOT/server2_logs.txt"
# Procuramos pela lógica no ServerSynchronizationController
if grep -i "findAndLockPartner" "$PROJECT_ROOT/server2_logs.txt"; then
  echo -e "\e[32m>>> SUCESSO: Server-2 recebeu solicitação de pareamento (findAndLockPartner).\e[0m"
else
  echo -e "\e[33m>>> AVISO: Não foi encontrado log de recebimento de pareamento no server-2.\e[0m"
fi

echo ">>> Verificando confirmação de início de jogo (GAME_START)..."
# Ambos os servidores devem logar o início do jogo
if grep -i "UPDATE:GAME_START" "$PROJECT_ROOT/server1_logs.txt" || grep -i "UPDATE:GAME_START" "$PROJECT_ROOT/server2_logs.txt"; then
  echo -e "\e[32m>>> SUCESSO FINAL: Mensagem 'GAME_START' encontrada nos logs! O pareamento distribuído funcionou.\e[0m"
else
  echo -e "\e[31m>>> FALHA: Mensagem 'GAME_START' não encontrada em nenhum dos servidores.\e[0m"
  echo "--- Logs Server 1 (Últimas 50 linhas) ---"
  tail -n 50 "$PROJECT_ROOT/server1_logs.txt"
  echo "--- Logs Server 2 (Últimas 50 linhas) ---"
  tail -n 50 "$PROJECT_ROOT/server2_logs.txt"
fi

echo "======================================================="
echo ">>> TESTE DE PAREAMENTO CROSS-SERVIDOR CONCLUÍDO"
echo "======================================================="

# A limpeza (cleanup) será chamada automaticamente na saída