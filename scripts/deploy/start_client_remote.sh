#!/bin/bash
# Script para iniciar cliente em máquina remota
echo "=== Iniciar Cliente Remoto ==="
echo "Certifique-se de que o gateway está rodando em outro computador"

# Compilar se necessário
if [ ! -f "dueling-client/target/dueling-client-1.0-SNAPSHOT.jar" ]; then
    echo "Compilando cliente..."
    cd ..
    mvn clean package -DskipTests
    cd dueling-client
fi

read -p "IP do gateway: " GATEWAY_HOST
read -p "Porta do gateway (padrão 8080): " -e -i "8080" GATEWAY_PORT

WEBSOCKET_URL="ws://$GATEWAY_HOST:$GATEWAY_PORT/ws"

cd dueling-client
java -DGATEWAY_HOST=$GATEWAY_HOST \
     -DGATEWAY_PORT=$GATEWAY_PORT \
     -DWEBSOCKET_URL=$WEBSOCKET_URL \
     -jar target/dueling-client-1.0-SNAPSHOT.jar