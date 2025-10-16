#!/bin/bash

# Script para executar o cliente do Protocolo de Duelo
echo "Executando cliente do Protocolo de Duelo..."

# Diretório base do projeto (relative to script location)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$(dirname "$SCRIPT_DIR")")"

# Verificar se o diretório do projeto existe
if [ ! -d "$PROJECT_DIR" ]; then
    echo "Erro: Diretório do projeto não encontrado em $PROJECT_DIR"
    exit 1
fi

# Navegar para o diretório do projeto
cd "$PROJECT_DIR"

# Verificar se os serviços estão rodando
echo "Verificando se os serviços estão rodando..."
if ! netstat -tulpn 2>/dev/null | grep -q ":8080.*LISTEN"; then
    echo "Erro: Gateway não está rodando na porta 8080"
    echo "Por favor, inicie o sistema com: ./start_complete_system.sh"
    exit 1
fi

# Navegar para o diretório do cliente
cd dueling-client

# Compilar o cliente se necessário
if [ ! -f "target/dueling-client-1.0-SNAPSHOT.jar" ]; then
    echo "Compilando cliente..."
    mvn clean package -DskipTests
fi

# Executar o cliente
echo "Iniciando cliente..."
GATEWAY_HOST=localhost \
GATEWAY_PORT=8080 \
WEBSOCKET_URL=ws://localhost:8080/ws \
java -jar target/dueling-client-1.0-SNAPSHOT.jar