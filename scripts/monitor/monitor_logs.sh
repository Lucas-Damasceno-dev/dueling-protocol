#!/bin/bash

# Script para monitorar os logs dos serviços do Protocolo de Duelo
echo "=== Monitor de Logs do Protocolo de Duelo ==="

# Diretório base do projeto
PROJECT_DIR="/home/lucas/Documentos/dev/projects/dueling-protocol"

# Verificar se o diretório do projeto existe
if [ ! -d "$PROJECT_DIR" ]; then
    echo "Erro: Diretório do projeto não encontrado em $PROJECT_DIR"
    exit 1
fi

# Navegar para o diretório do projeto
cd "$PROJECT_DIR"

# Função para monitorar logs
monitor_logs() {
    local service_name=$1
    local log_file=$2
    
    if [ -f "$log_file" ]; then
        echo "Monitorando logs do $service_name ($log_file):"
        tail -f "$log_file" | while read line; do
            echo "[$service_name] $line"
        done
    else
        echo "Arquivo de log não encontrado para $service_name: $log_file"
    fi
}

# Verificar se os serviços estão rodando
echo "Verificando serviços em execução..."
if netstat -tulpn 2>/dev/null | grep -q ":8080.*LISTEN"; then
    echo "✓ Gateway está rodando na porta 8080"
else
    echo "✗ Gateway não está rodando na porta 8080"
fi

if netstat -tulpn 2>/dev/null | grep -q ":8083.*LISTEN"; then
    echo "✓ Servidor está rodando na porta 8083"
else
    echo "✗ Servidor não está rodando na porta 8083"
fi

echo ""
echo "Iniciando monitoramento de logs (Ctrl+C para parar):"
echo ""

# Monitorar logs em segundo plano
monitor_logs "Gateway" "$PROJECT_DIR/dueling-gateway/gateway.log" &
GATEWAY_LOG_PID=$!

monitor_logs "Servidor" "$PROJECT_DIR/dueling-server/server.log" &
SERVER_LOG_PID=$!

# Função para limpar processos filhos ao sair
cleanup() {
    echo ""
    echo "Parando monitoramento de logs..."
    kill $GATEWAY_LOG_PID $SERVER_LOG_PID 2>/dev/null || true
    exit 0
}

# Capturar sinal de interrupção (Ctrl+C)
trap cleanup INT

# Manter o script em execução
wait