#!/bin/bash

# Script para monitorar os logs dos serviços do Protocolo de Duelo
echo "=== Monitor de Logs do Protocolo de Duelo ==="

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

# Função para monitorar logs de arquivos
monitor_file_logs() {
    local service_name=$1
    local log_file=$2
    
    if [ -f "$log_file" ]; then
        echo "Monitorando logs do $service_name ($log_file):"
        tail -f "$log_file" 2>/dev/null || echo "[ERROR] Não foi possível acessar $log_file"
    else
        echo "Arquivo de log não encontrado para $service_name: $log_file (continuando com outros logs...)"
    fi
}

# Função para monitorar logs do Docker
monitor_docker_logs() {
    if command -v docker &> /dev/null && docker compose &> /dev/null; then
        echo "Monitorando logs do Docker (todos os containers):"
        docker compose -f "$PROJECT_DIR/docker/docker-compose.yml" logs -f --tail=50
    else
        echo "Docker ou docker-compose não está disponível (continuando com outros logs...)"
    fi
}

# Função para monitorar logs de processos Java do cliente em segundo plano
monitor_java_client() {
    while true; do
        # Procurar por processos Java do cliente e seus PIDs
        CLIENT_PIDS=$(pgrep -f "dueling-client-1.0-SNAPSHOT.jar" 2>/dev/null || true)
        if [ -n "$CLIENT_PIDS" ]; then
            # Para cada processo cliente encontrado, exibir uma mensagem
            for pid in $CLIENT_PIDS; do
                if [ -d "/proc/$pid" ]; then
                    echo "[CLIENT-JAVA-PID:$pid] Cliente Java está em execução (use 'jcmd $pid Thread.print' ou 'kill -USR1 $pid' para informações adicionais)"
                fi
            done
        else
            echo "[CLIENT-JAVA] Nenhum processo cliente Java encontrado no momento"
        fi
        sleep 5
    done
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

# Verificar se containers Docker estão rodando
if command -v docker &> /dev/null && docker ps &> /dev/null; then
    DOCKER_CONTAINERS=$(docker compose -f "$PROJECT_DIR/docker/docker-compose.yml" ps -q 2>/dev/null | wc -l)
    if [ "$DOCKER_CONTAINERS" -gt 0 ]; then
        echo "✓ $DOCKER_CONTAINERS containers Docker estão em execução"
    else
        echo "✗ Nenhum container Docker encontrado"
    fi
else
    echo "✗ Docker não está disponível ou não está em execução"
fi

# Verificar se processos Java do cliente estão rodando
CLIENT_PIDS=$(pgrep -f "dueling-client-1.0-SNAPSHOT.jar" 2>/dev/null || true)
if [ -n "$CLIENT_PIDS" ]; then
    echo "✓ Processo(s) cliente Java está(ão) rodando: $CLIENT_PIDS"
else
    echo "✗ Nenhum processo cliente Java encontrado"
fi

echo ""
echo "Iniciando monitoramento de logs (Ctrl+C para parar):"
echo ""

# Iniciar monitoramento de processos Java do cliente em segundo plano
monitor_java_client &
CLIENT_MONITOR_PID=$!

# Iniciar monitoramento de logs do Docker em segundo plano, se estiver disponível
if command -v docker &> /dev/null && docker compose &> /dev/null; then
    monitor_docker_logs &
    DOCKER_LOG_PID=$!
fi

# Monitorar logs em segundo plano
monitor_file_logs "Gateway" "$PROJECT_DIR/dueling-gateway/gateway.log" &
GATEWAY_LOG_PID=$!

monitor_file_logs "Servidor" "$PROJECT_DIR/dueling-server/server.log" &
SERVER_LOG_PID=$!

# Função para limpar processos filhos ao sair
cleanup() {
    echo ""
    echo "Parando monitoramento de logs..."
    kill $GATEWAY_LOG_PID $SERVER_LOG_PID $CLIENT_MONITOR_PID 2>/dev/null || true
    if [ -n "$DOCKER_LOG_PID" ] && kill -0 $DOCKER_LOG_PID 2>/dev/null; then
        kill $DOCKER_LOG_PID 2>/dev/null || true
    fi
    exit 0
}

# Capturar sinal de interrupção (Ctrl+C)
trap cleanup INT

# Manter o script em execução
wait