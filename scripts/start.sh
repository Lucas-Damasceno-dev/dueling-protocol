#!/bin/bash

# Script unificado para iniciar o sistema Dueling Protocol
# Modos disponíveis: docker, local, simple, debug, final, no-redis

MODE="docker" # Modo padrão

# Processar argumentos da linha de comando
if [ "$1" == "--mode" ] && [ -n "$2" ]; then
    MODE=$2
fi

echo "=== Protocolo de Duelo - Iniciando no modo: $MODE ==="

# Diretório base do projeto
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_DIR"

# Parar quaisquer serviços em execução antes de iniciar um novo modo
echo "Parando serviços antigos..."
./scripts/deploy/stop_all_services.sh 2>/dev/null || true
sleep 5

case $MODE in
    docker)
        echo "Compilando o projeto..."
        mvn clean package -DskipTests

        echo "Iniciando sistema com NGINX como gateway..."
        cd docker

        if [ -f .env ]; then
            export $(cat .env | sed 's/#.*//g' | xargs)
        fi

        docker compose up --build -d

        echo "Aguardando serviços iniciarem (isso pode levar até 2 minutos)..."
        sleep 120

        if docker compose ps | grep -q "nginx-gateway.*Up.*healthy"; then
            echo ""
            echo "=== Sistema iniciado com sucesso ==="
            echo "NGINX Gateway: http://localhost:8080"
            echo "WebSocket: ws://localhost:8080/ws"
            # ... (o resto da mensagem de sucesso)
        else
            echo ""
            echo "=== Erro ao iniciar o sistema ==="
            docker compose ps
            # ... (o resto da mensagem de erro)
        fi
        ;;
    local)
        echo "Verificando serviços de infraestrutura..."
        if ! docker ps | grep -q postgres; then
            echo "Iniciando PostgreSQL e Redis..."
            cd docker && docker compose up postgres redis-master redis-sentinel-1 redis-sentinel-2 redis-sentinel-3 redis-slave -d
            cd ..
            echo "Aguardando PostgreSQL e Redis iniciarem..."
            sleep 15
        else
            echo "PostgreSQL e Redis já estão rodando."
        fi

        echo "Compilando o projeto..."
        if [ ! -f "dueling-gateway/target/dueling-gateway-1.0-SNAPSHOT.jar" ] || [ ! -f "dueling-server/target/dueling-server-1.0-SNAPSHOT.jar" ] || [ ! -f "dueling-client/target/dueling-client-1.0-SNAPSHOT.jar" ]; then
            mvn clean package -DskipTests
        fi

        echo "Iniciando Servidor na porta 8080..."
        cd dueling-server
        java -DSERVER_PORT=8080 \
             -DPOSTGRES_HOST=localhost \
             -DPOSTGRES_PORT=5432 \
             -DPOSTGRES_DB=dueling_db \
             -DPOSTGRES_USER=dueling_user \
             -DPOSTGRES_PASSWORD=dueling_password \
             -Dspring.profiles.active=local-dev,server,default \
             -Dspring.redis.host=localhost \
             -Dspring.redis.port=6379 \
             -jar target/dueling-server-1.0-SNAPSHOT.jar > server.log 2>&1 &
        SERVER_PID=$!
        cd ..

        echo "Servidor iniciado com PID: $SERVER_PID"
        sleep 25
        echo ""
        echo "=== Servidor iniciado com sucesso ==="
        echo "Servidor: http://localhost:8080"
        echo "WebSocket: ws://localhost:8080/ws"
        echo ""
        echo "Para iniciar o cliente, execute: ./run_client.sh"
        echo ""
        echo "Para parar todos os serviços, execute: ./stop_all_services.sh"
        ;;
    simple)
        echo "Verificando serviços de infraestrutura..."
        if ! docker ps | grep -q postgres; then
            echo "Iniciando PostgreSQL e Redis (modo simples)..."
            cd docker && docker compose up postgres redis-master -d
            cd ..
            echo "Aguardando PostgreSQL e Redis iniciarem..."
            sleep 10
        else
            echo "PostgreSQL e Redis já estão rodando."
        fi

        echo "Compilando o projeto..."
        if [ ! -f "dueling-gateway/target/dueling-gateway-1.0-SNAPSHOT.jar" ] || [ ! -f "dueling-server/target/dueling-server-1.0-SNAPSHOT.jar" ] || [ ! -f "dueling-client/target/dueling-client-1.0-SNAPSHOT.jar" ]; then
            mvn clean package -DskipTests
        fi

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
        sleep 10

        echo "Iniciando Servidor na porta 8083..."
        cd dueling-server
        java -DSERVER_PORT=8083 \
             -DPOSTGRES_HOST=localhost \
             -DPOSTGRES_PORT=5432 \
             -DPOSTGRES_DB=dueling_db \
             -DPOSTGRES_USER=dueling_user \
             -DPOSTGRES_PASSWORD=dueling_password \
             -Dspring.profiles.active=server,default,local-dev \
             -Dspring.redis.host=localhost \
             -Dspring.redis.port=6379 \
             -Dspring.redis.sentinel.master= \
             -Dspring.redis.sentinel.nodes= \
             -jar target/dueling-server-1.0-SNAPSHOT.jar > server.log 2>&1 &
        SERVER_PID=$!
        cd ..

        echo "Servidor iniciado com PID: $SERVER_PID"
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
        ;;
    debug)
        echo "Verificando serviços de infraestrutura..."
        if ! docker ps | grep -q postgres; then
            echo "Iniciando PostgreSQL e Redis..."
            cd docker && docker compose up postgres redis-master -d
            cd ..
            echo "Aguardando PostgreSQL e Redis iniciarem..."
            sleep 10
        else
            echo "PostgreSQL e Redis já estão rodando."
        fi

        echo "Compilando o projeto..."
        if [ ! -f "dueling-gateway/target/dueling-gateway-1.0-SNAPSHOT.jar" ] || [ ! -f "dueling-server/target/dueling-server-1.0-SNAPSHOT.jar" ] || [ ! -f "dueling-client/target/dueling-client-1.0-SNAPSHOT.jar" ]; then
            mvn clean package -DskipTests
        fi

        echo "Iniciando Gateway na porta 8080..."
        cd dueling-gateway
        java -DGATEWAY_HOST=localhost \
             -DGATEWAY_PORT=8080 \
             -DSERVER_HOST=localhost \
             -DSERVER_PORT=8083 \
             -DJWT_SECRET=mySecretKeyForDuelingProtocolThatShouldBeLongerThan256Bits \
             -jar target/dueling-gateway-1.0-SNAPSHOT.jar > gateway.log 2>&1 &
        GATEWAY_PID=$!
        cd ..

        echo "Gateway iniciado com PID: $GATEWAY_PID"
        sleep 12

        echo "Iniciando Servidor na porta 8083..."
        cd dueling-server
        java -DSERVER_PORT=8083 \
             -DPOSTGRES_HOST=localhost \
             -DPOSTGRES_PORT=5432 \
             -DPOSTGRES_DB=dueling_db \
             -DPOSTGRES_USER=dueling_user \
             -DPOSTGRES_PASSWORD=dueling_password \
             -Dspring.profiles.active=server,default \
             -Dspring.redis.host=localhost \
             -Dspring.redis.port=6379 \
             -DJWT_SECRET=mySecretKeyForDuelingProtocolThatShouldBeLongerThan256Bits \
             -Dlogging.level.websocket=DEBUG \
             -Dlogging.level.security=DEBUG \
             -Dlogging.level.org.springframework.web.socket=DEBUG \
             -jar target/dueling-server-1.0-SNAPSHOT.jar > server.log 2>&1 &
        SERVER_PID=$!
        cd ..

        echo "Servidor iniciado com PID: $SERVER_PID"
        echo "Para parar todos os serviços, execute: ./stop_all_services.sh"
        ;;
    final)
        echo "Verificando serviços de infraestrutura..."
        if ! docker ps | grep -q postgres; then
            echo "Iniciando PostgreSQL e Redis Sentinel..."
            cd docker && docker compose up postgres redis-master redis-sentinel-1 redis-sentinel-2 redis-sentinel-3 redis-slave -d
            cd ..
            echo "Aguardando PostgreSQL e Redis iniciarem..."
            sleep 15
        else
            echo "PostgreSQL e Redis já estão rodando."
        fi

        echo "Compilando o projeto..."
        if [ ! -f "dueling-gateway/target/dueling-gateway-1.0-SNAPSHOT.jar" ] || [ ! -f "dueling-server/target/dueling-server-1.0-SNAPSHOT.jar" ] || [ ! -f "dueling-client/target/dueling-client-1.0-SNAPSHOT.jar" ]; then
            mvn clean package -DskipTests
        fi

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
        sleep 10

        echo "Iniciando Servidor na porta 8083..."
        cd dueling-server
        java -DSERVER_PORT=8083 \
             -DPOSTGRES_HOST=localhost \
             -DPOSTGRES_PORT=5432 \
             -DPOSTGRES_DB=dueling_db \
             -DPOSTGRES_USER=dueling_user \
             -DPOSTGRES_PASSWORD=dueling_pass \
             -Dspring.profiles.active=server,default \
             -Dspring.redis.sentinel.master=mymaster \
             -Dspring.redis.sentinel.nodes=localhost:26379,localhost:26380,localhost:26381 \
             -Ddueling.redis.sentinel.master=mymaster \
        echo "Para parar todos os serviços, execute: ./stop_all_services.sh"
        ;;
    no-redis)
        echo "Verificando serviços de infraestrutura..."
        if ! docker ps | grep -q postgres; then
            echo "Iniciando PostgreSQL e Redis Sentinel..."
            cd docker && docker compose up postgres redis-master redis-sentinel-1 redis-sentinel-2 redis-sentinel-3 redis-slave -d
            cd ..
            echo "Aguardando PostgreSQL e Redis iniciarem..."
            sleep 15
        else
            echo "PostgreSQL e Redis já estão rodando."
        fi

        echo "Compilando o projeto..."
        if [ ! -f "dueling-gateway/target/dueling-gateway-1.0-SNAPSHOT.jar" ] || [ ! -f "dueling-server/target/dueling-server-1.0-SNAPSHOT.jar" ] || [ ! -f "dueling-client/target/dueling-client-1.0-SNAPSHOT.jar" ]; then
            mvn clean package -DskipTests
        fi

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
        sleep 10

        echo "Iniciando Servidor na porta 8083 com perfil local-dev + server + default..."
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
        ;;



    
    *)
        echo "Modo inválido: $MODE"
        echo "Modos disponíveis: docker, local, simple, debug, final, no-redis"
        exit 1
        ;;
esac
