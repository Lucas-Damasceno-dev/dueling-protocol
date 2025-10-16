#!/usr/bin/env bash

# test_scripts/infrastructure/test_redis_failover.sh

# Garante que o script pára se qualquer comando falhar
set -e

# Define o caminho para o ficheiro docker-compose
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
PROJECT_ROOT="$SCRIPT_DIR/../.."
DOCKER_COMPOSE_FILE="$PROJECT_ROOT/docker/docker-compose.yml"

# Função de limpeza para parar e remover os containers
cleanup() {
  echo ""
  echo ">>> [CLEANUP] Parando e removendo containers..."
  docker compose -f "$DOCKER_COMPOSE_FILE" down --remove-orphans -v
  echo ">>> [CLEANUP] Ambiente limpo."
}

# Define a função de limpeza para ser executada na saída do script
trap cleanup EXIT

echo "======================================================="
echo ">>> INICIANDO TESTE DE FAILOVER DO REDIS SENTINEL"
echo "======================================================="

# 1. Preparação (Setup)
echo ">>> [SETUP] Iniciando todos os serviços Docker..."
docker compose -f "$DOCKER_COMPOSE_FILE" up -d

echo ">>> [SETUP] Aguardando 30 segundos para a estabilização dos serviços..."
sleep 30

# 2. Passo 1: Verificar Estado Inicial
echo ">>> [PASSO 1] Verificando estado inicial do cluster Redis..."

# A porta do redis-master dentro da rede docker é 6379
MASTER_ADDR_INFO=$(docker exec redis-sentinel-1 redis-cli -p 26379 sentinel get-master-addr-by-name mymaster)
MASTER_IP=$(echo "$MASTER_ADDR_INFO" | head -n 1)
MASTER_PORT=$(echo "$MASTER_ADDR_INFO" | tail -n 1)

echo "Endereço do mestre detetado pelo Sentinel: $MASTER_IP:$MASTER_PORT"

# No docker-compose, o redis-master é exposto como 'redis-master' e porta 6379
if ! [[ "$MASTER_IP" == *"redis-master"* || "$MASTER_PORT" == "6379" ]]; then
    echo ">>> [ERRO] O mestre inicial não é 'redis-master:6379'. Teste abortado."
    exit 1
fi
echo ">>> [PASSO 1] Sucesso: Mestre inicial ('redis-master') confirmado."

echo ">>> [PASSO 1] Escrevendo dados no mestre..."
docker exec redis-master redis-cli set failover_test "dados_antes_do_failover"

echo ">>> [PASSO 1] Aguardando 2 segundos para replicação..."
sleep 2

echo ">>> [PASSO 1] Verificando dados no escravo (porta 6380)..."
SLAVE_DATA=$(docker exec redis-slave redis-cli -p 6380 get failover_test)

if [ "$SLAVE_DATA" = "dados_antes_do_failover" ]; then
    echo ">>> [PASSO 1] Sucesso: Dados replicados corretamente para o escravo."
else
    echo ">>> [ERRO] Falha na replicação de dados. Valor no escravo: '$SLAVE_DATA'"
    exit 1
fi

# 3. Passo 2: Simular Falha do Mestre
echo ">>> [PASSO 2] Simulando falha: Parando 'redis-master'..."
docker stop redis-master
echo ">>> [PASSO 2] Container 'redis-master' parado."

# 4. Passo 3: Verificar Failover do Sentinel
echo ">>> [PASSO 3] Aguardando 45 segundos para deteção de falha e eleição do novo mestre..."
sleep 45

echo ">>> [PASSO 3] Verificando novo mestre..."
NEW_MASTER_ADDR_INFO=$(docker exec redis-sentinel-1 redis-cli -p 26379 sentinel get-master-addr-by-name mymaster)
NEW_MASTER_IP=$(echo "$NEW_MASTER_ADDR_INFO" | head -n 1)
NEW_MASTER_PORT=$(echo "$NEW_MASTER_ADDR_INFO" | tail -n 1)

echo "Novo endereço do mestre detetado pelo Sentinel: $NEW_MASTER_IP:$NEW_MASTER_PORT"

# O redis-slave está em 'redis-slave' e porta 6379 (interna) ou 6380 (exposta)
# O sentinel reportará a porta interna 6379.
if [[ "$NEW_MASTER_IP" == *"redis-slave"* && "$NEW_MASTER_PORT" == "6379" ]]; then
    echo ">>> [PASSO 3] SUCESSO: Failover concluído. 'redis-slave' foi promovido a mestre."
else
    echo ">>> [ERRO] Falha no failover. Novo mestre: $NEW_MASTER_IP:$NEW_MASTER_PORT"
    echo "Logs do Sentinel:"
    docker logs redis-sentinel-1 | tail -n 20
    exit 1
fi

# 5. Passo 4: Verificar Resiliência da Aplicação
echo ">>> [PASSO 4] Verificando logs dos servidores da aplicação por atividade de failover..."
# Aguarda um pouco mais para os servidores se reconectarem
sleep 10
LOGS_SERVER_1=$(docker logs server-1 2>&1 | tail -n 50)
LOGS_SERVER_2=$(docker logs server-2 2>&1 | tail -n 50)

if echo "$LOGS_SERVER_1" | grep -i -E "failover|reconnected|switched to master"; then
    echo ">>> [PASSO 4] Sucesso: 'server-1' detetou o failover."
else
    echo ">>> [PASSO 4] AVISO: Nenhuma mensagem de failover explícita encontrada em 'server-1'."
fi

if echo "$LOGS_SERVER_2" | grep -i -E "failover|reconnected|switched to master"; then
    echo ">>> [PASSO 4] Sucesso: 'server-2' detetou o failover."
else
    echo ">>> [PASSO 4] AVISO: Nenhuma mensagem de failover explícita encontrada em 'server-2'."
fi

# 6. Passo 5: Verificar Integridade dos Dados
echo ">>> [PASSO 5] Verificando integridade dos dados no novo mestre (redis-slave na porta 6380)..."
DATA_POST_FAILOVER=$(docker exec redis-slave redis-cli -p 6380 get failover_test)

if [ "$DATA_POST_FAILOVER" = "dados_antes_do_failover" ]; then
    echo ">>> [PASSO 5] Sucesso: Dados preservados após o failover."
else
    echo ">>> [ERRO] Perda de dados detetada. Valor no novo mestre: '$DATA_POST_FAILOVER'"
    exit 1
fi

echo ">>> [PASSO 5] Tentando escrever novos dados no novo mestre..."
docker exec redis-slave redis-cli -p 6380 set failover_test_2 "dados_apos_failover"
NEW_DATA=$(docker exec redis-slave redis-cli -p 6380 get failover_test_2)

if [ "$NEW_DATA" = "dados_apos_failover" ]; then
    echo ">>> [PASSO 5] Sucesso: Escrita no novo mestre foi bem-sucedida."
else
    echo ">>> [ERRO] Falha ao escrever no novo mestre."
    exit 1
fi

echo "======================================================="
echo ">>> SUCESSO: Teste de failover do Redis Sentinel concluído."
echo "======================================================="