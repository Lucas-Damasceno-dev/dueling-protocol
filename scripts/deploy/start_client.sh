#!/bin/bash

# Script para iniciar o cliente do Protocolo de Duelo

echo "Iniciando cliente do Protocolo de Duelo..."
echo "Certifique-se de que o gateway e o servidor estão rodando antes de iniciar o cliente!"

cd /home/lucas/Documentos/dev/projects/dueling-protocol/dueling-client

# Compilar se necessário
if [ ! -f "target/dueling-client-1.0-SNAPSHOT.jar" ]; then
    echo "Compilando cliente..."
    cd ..
    mvn clean package -DskipTests
    cd dueling-client
fi

echo "Executando cliente..."
java -DGATEWAY_HOST=localhost \
     -DGATEWAY_PORT=8080 \
     -DWEBSOCKET_URL=ws://localhost:8080/ws \
     -jar target/dueling-client-1.0-SNAPSHOT.jar