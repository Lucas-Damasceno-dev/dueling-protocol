#!/bin/bash

# Script para testar comunicação Servidor-Servidor (S2S) no projeto "Dueling Protocol"
# Este script executa os seguintes passos:
# 1. Limpeza de containers em execução
# 2. Inicialização parcial dos serviços necessários
# 3. Espera para inicialização completa
# 4. Captura e análise de logs para provar comunicação S2S
# 5. Limpeza final

set -e  # Sai imediatamente se um comando falhar

echo "Iniciando script de teste de comunicação S2S..."

# Função para limpeza final (garantida mesmo em caso de falha)
cleanup() {
    echo "Executando limpeza final..."
    cd /home/lucas/Documentos/dev/projects/dueling-protocol/docker
    docker compose -f docker-compose.yml down --remove-orphans || true
    # Remover arquivos de log antigos
    rm -f server1.log server2.log || true
}
# Configurar trap para executar a limpeza em caso de saída inesperada
trap cleanup EXIT

# Mudar para o diretório docker onde está o docker-compose.yml
cd /home/lucas/Documentos/dev/projects/dueling-protocol/docker

# Etapa 1: Limpeza
echo "1. Limpando containers em execução..."
docker compose -f docker-compose.yml down --remove-orphans || true

# Remover arquivos de log antigos
rm -f server1.log server2.log || true

# Etapa 2: Inicialização Parcial
echo "2. Iniciando containers (postgres, redis-master, server-1, server-2)..."
docker compose -f docker-compose.yml up -d --build postgres redis-master redis-sentinel-1 redis-sentinel-2 redis-sentinel-3 server-1 server-2

# Etapa 3: Espera
echo "3. Aguardando 45s para inicialização completa dos servidores..."
sleep 45

# Etapa 4: Captura de Logs
echo "4. Capturando logs dos servidores..."
docker compose -f docker-compose.yml logs server-1 > server1.log 2>&1
docker compose -f docker-compose.yml logs server-2 > server2.log 2>&1

# Etapa 5: Análise e Verificação
echo "5. Analisando logs para prova de comunicação S2S..."

# Verificar se algum servidor tentou registrar com o peer
RESULT_1=$(grep -i "Attempting to register with peer" server1.log || true)
if [ -z "$RESULT_1" ]; then
    RESULT_1=$(grep -i "Attempting to register with peer" server2.log || true)
fi

# Verificar se algum servidor recebeu a chamada de registro
RESULT_2=$(grep 'POST "/api/servers/register"' server2.log || true)
if [ -z "$RESULT_2" ]; then
    RESULT_2=$(grep 'POST "/api/servers/register"' server1.log || true)
fi

echo "Resultado análise (tentativa de envio):"
if [ -n "$RESULT_1" ]; then
    echo "   [ENCONTRADO] '$RESULT_1'"
else
    echo "   [NÃO ENCONTRADO] 'Attempting to register with peer'"
    # Print any registration-related logs for debugging
    echo "   [DEBUG] Other registration related logs in server1.log:"
    grep -i "register" server1.log | head -5
    echo "   [DEBUG] Other registration related logs in server2.log:"
    grep -i "register" server2.log | head -5
fi

echo "Resultado análise (recepção da chamada):"
if [ -n "$RESULT_2" ]; then
    echo "   [ENCONTRADO] '$RESULT_2'"
else
    echo "   [NÃO ENCONTRADO] 'POST /api/servers/register'"
    # Print any access logs for the registration endpoint
    echo "   [DEBUG] Looking for other server communication logs in server1:"
    grep -i "server" server1.log | grep -v "register with peer" | head -5
    echo "   [DEBUG] Looking for other server communication logs in server2:"
    grep -i "server" server2.log | grep -v "register with peer" | head -5
fi

# Etapa 6: Determinar sucesso ou falha
if [ -n "$RESULT_1" ] && [ -n "$RESULT_2" ]; then
    echo ""
    echo "[SUCESSO] Comunicação S2S explícita demonstrada."
    exit 0
else
    echo ""
    echo "[FALHA] Não foi possível encontrar logs de comunicação S2S."
    exit 1
fi