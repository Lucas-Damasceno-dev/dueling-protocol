#!/bin/bash
set -e

echo "Iniciando NGINX Gateway..."

# Copiar o arquivo nginx.conf existente para backup
cp /etc/nginx/nginx.conf /etc/nginx/nginx.conf.backup

# Verificar se os servidores estão prontos antes de iniciar o NGINX
echo "Aguardando servidores estarem prontos..."

# Função para verificar se um servidor está saudável
check_server_health() {
    local server=$1
    local port=${2:-8080}
    
    # Tentar verificar a saúde do servidor
    if curl -f http://$server:$port/actuator/health >/dev/null 2>&1; then
        return 0
    else
        return 1
    fi
}

# Aguardar até que todos os servidores estejam saudáveis
SERVERS=("server-1" "server-2" "server-3" "server-4")
ALL_SERVERS_READY=false
MAX_ATTEMPTS=60
ATTEMPT=0

while [ $ATTEMPT -lt $MAX_ATTEMPTS ] && [ "$ALL_SERVERS_READY" = false ]; do
    ALL_SERVERS_READY=true
    
    for server in "${SERVERS[@]}"; do
        if check_server_health "$server" 8080; then
            echo "$server está pronto!"
        else
            echo "$server ainda não está pronto, aguardando..."
            ALL_SERVERS_READY=false
        fi
    done
    
    if [ "$ALL_SERVERS_READY" = false ]; then
        ATTEMPT=$((ATTEMPT + 1))
        sleep 2
    fi
done

if [ "$ALL_SERVERS_READY" = false ]; then
    echo "Aviso: Nem todos os servidores estão prontos após $MAX_ATTEMPTS tentativas, iniciando NGINX mesmo assim..."
fi

echo "Iniciando NGINX com configuração estática..."
nginx -g "daemon off;"