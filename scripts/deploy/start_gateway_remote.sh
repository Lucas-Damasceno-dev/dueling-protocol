#!/bin/bash
# Script para iniciar gateway em máquina remota
echo "=== Iniciar Gateway Remoto ==="
echo "Certifique-se de que o servidor está rodando na máquina especificada"

# Compilar se necessário
if [ ! -f "dueling-gateway/target/dueling-gateway-1.0-SNAPSHOT.jar" ]; then
    echo "Compilando gateway..."
    mvn clean package -DskipTests
fi

read -p "IP do servidor: " SERVER_HOST
read -p "Porta do servidor (padrão 8083): " -e -i "8083" SERVER_PORT
read -p "Porta do gateway (padrão 8080): " -e -i "8080" GATEWAY_PORT

cd dueling-gateway
java -DGATEWAY_HOST=0.0.0.0 \
     -DGATEWAY_PORT=$GATEWAY_PORT \
     -DSERVER_HOST=$SERVER_HOST \
     -DSERVER_PORT=$SERVER_PORT \
     -jar target/dueling-gateway-1.0-SNAPSHOT.jar