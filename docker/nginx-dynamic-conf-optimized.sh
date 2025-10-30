#!/bin/bash
set -e

echo "Iniciando NGINX Gateway..."

# Create a backup of the original nginx configuration
cp /etc/nginx/nginx.conf /etc/nginx/nginx.conf.backup

echo "Aguardando servidores estarem prontos..."

# Function to check if a server is healthy
check_server_health() {
    local server=$1
    local port=${2:-8080}
    
    if curl -f "http://$server:$port/actuator/health" >/dev/null 2>&1; then
        return 0
    else
        return 1
    fi
}

# Check which servers are available and healthy
AVAILABLE_SERVERS=()
ALL_SERVERS=("server-1" "server-2" "server-3" "server-4")

echo "Verificando servidores disponíveis..."

for server in "${ALL_SERVERS[@]}"; do
    if getent hosts "$server" >/dev/null 2>&1; then
        echo "Servidor $server encontrado, verificando saúde..."
        if check_server_health "$server" 8080; then
            AVAILABLE_SERVERS+=("$server")
            echo "$server está saudável!"
        else
            # Still add it even if not immediately healthy, we'll use it if needed
            AVAILABLE_SERVERS+=("$server")
            echo "$server encontrado mas não está saudável ainda (irá tentar mais tarde)"
        fi
    else
        echo "Servidor $server não encontrado, pulando..."
    fi
done

echo "Servidores disponíveis: ${AVAILABLE_SERVERS[*]}"

# Wait for at least one server to be healthy before starting NGINX
if [ ${#AVAILABLE_SERVERS[@]} -gt 0 ]; then
    echo "Aguardando pelo menos um servidor ficar disponível..."
    ATTEMPT=0
    MAX_ATTEMPTS=20  # Reduced attempts since we're more flexible
    
    while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
        for server in "${AVAILABLE_SERVERS[@]}"; do
            if check_server_health "$server" 8080; then
                # Create a temporary nginx configuration with this server as primary
                echo "Servidor $server está pronto para uso."
                
                # We'll use the original configuration which has dynamic server resolution
                # but we can modify which server is used as the default
                sed -i "s/set \$backend \"[^\"]*\";/set \$backend \"$server\";/1" /etc/nginx/nginx.conf
                break 2  # Exit both loop and if
            fi
        done
        
        ATTEMPT=$((ATTEMPT + 1))
        echo "Aguardando servidores... tentativa $ATTEMPT/$MAX_ATTEMPTS"
        sleep 3
    done
else
    echo "Nenhum servidor encontrado, usando server-1 como fallback..."
    sed -i "s/set \$backend \"[^\"]*\";/set \$backend \"server-1\";/1" /etc/nginx/nginx.conf
fi

echo "Iniciando NGINX com servidores detectados..."
nginx -g "daemon off;"