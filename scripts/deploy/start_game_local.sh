#!/bin/bash

# Script para iniciar o jogo Protocolo de Duelo localmente com Redis local
echo "=== Protocolo de Duelo - Iniciar Jogo Localmente (com Redis Local) ==="

# Verificar se os serviços de infraestrutura estão rodando
echo "Verificando serviços de infraestrutura..."

# Tenta iniciar PostgreSQL e Redis se não estiverem rodando
if ! docker ps | grep -q postgres; then
    echo "Iniciando PostgreSQL e Redis..."
    cd docker && docker compose up postgres redis-master redis-sentinel-1 redis-sentinel-2 redis-sentinel-3 redis-slave -d
    cd ..
    echo "Aguardando PostgreSQL e Redis iniciarem..."
    sleep 15
else
    echo "PostgreSQL e Redis já estão rodando."
fi

# Compilar o projeto se necessário
echo "Compilando o projeto..."
if [ ! -f "dueling-gateway/target/dueling-gateway-1.0-SNAPSHOT.jar" ] || [ ! -f "dueling-server/target/dueling-server-1.0-SNAPSHOT.jar" ] || [ ! -f "dueling-client/target/dueling-client-1.0-SNAPSHOT.jar" ]; then
    mvn clean package -DskipTests
fi

# Iniciar o gateway
echo "Iniciando Gateway na porta 8080..."
cd dueling-gateway
java -DGATEWAY_HOST=localhost \
     -DGATEWAY_PORT=8080 \
     -DSERVER_HOST=localhost \
     -DSERVER_PORT=8083 \
     -jar target/dueling-gateway-1.0-SNAPSHOT.jar > gateway.log 2>&1 &
GATEWAY_PID=$!
cd ..

echo "Gateway iniciado com PID: $GATEWAY_PID"

# Aguardar o gateway iniciar
sleep 10

# Iniciar o servidor com perfil local mas mantendo componentes necessários
echo "Iniciando Servidor na porta 8083..."
cd dueling-server
java -DSERVER_PORT=8083 \
     -DPOSTGRES_HOST=localhost \
     -DPOSTGRES_PORT=5432 \
     -DPOSTGRES_DB=dueling_db \
     -DPOSTGRES_USER=dueling_user \
     -DPOSTGRES_PASSWORD=dueling_pass \
     -Dspring.profiles.active=local-dev,server,default \
     -Dspring.redis.host=localhost \
     -Dspring.redis.port=6379 \
     -jar target/dueling-server-1.0-SNAPSHOT.jar > server.log 2>&1 &
SERVER_PID=$!
cd ..

echo "Servidor iniciado com PID: $SERVER_PID"

# Aguardar o servidor iniciar completamente
sleep 25

echo ""
echo "=== Serviços iniciados com sucesso ==="
echo "Gateway: http://localhost:8080"
echo "WebSocket: ws://localhost:8080/ws"
echo "Servidor: http://localhost:8083"
echo ""
echo "Para iniciar o cliente, execute: ./run_client.sh"
echo ""
echo "Para parar todos os serviços, execute: ./stop_all_services.sh"