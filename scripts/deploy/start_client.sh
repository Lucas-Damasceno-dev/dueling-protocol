#!/bin/bash

# Script para iniciar o cliente do Protocolo de Duelo

echo "Iniciando cliente do Protocolo de Duelo..."
echo "Certifique-se de que o gateway e o servidor estão rodando antes de iniciar o cliente!"

# Diretório base do projeto (relative to script location)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_DIR/dueling-client"

# Compilar se necessário
if [ ! -f "target/dueling-client-1.0-SNAPSHOT.jar" ]; then
    echo "Compilando cliente..."
    cd "$PROJECT_DIR"
    mvn clean package -DskipTests
    cd "$PROJECT_DIR/dueling-client"
fi

echo "Executando cliente..."
java -DGATEWAY_HOST=localhost \
     -DGATEWAY_PORT=8080 \
     -DWEBSOCKET_URL=ws://localhost:8080/ws \
     -jar target/dueling-client-1.0-SNAPSHOT.jar