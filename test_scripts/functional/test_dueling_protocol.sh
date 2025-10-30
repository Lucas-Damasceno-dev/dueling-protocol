#!/bin/bash

# Script para testar o protocolo de duelo com todas as etapas em sequência

echo "Iniciando teste do Protocolo de Duelo..."

# Parar containers antigos se existirem
echo "Parando containers Redis antigos..."
docker stop redis-master redis-replica 2>/dev/null
docker rm redis-master redis-replica 2>/dev/null

# Iniciar Redis
echo "Iniciando Redis..."
docker run -d --name redis-master -p 6379:6379 redis:alpine redis-server --appendonly yes

# Criar uma cópia do PID do Redis para referência futura
REDIS_PID=$(docker ps -q -f name=redis-master)

echo "Redis iniciado com PID: $REDIS_PID"

# Compilar todos os módulos
echo "Compilando módulos..."
cd /home/lucas/Documentos/dev/projects/dueling-protocol

echo "Compilando gateway..."
cd dueling-gateway && mvn clean compile -q

echo "Compilando server..."
cd ../dueling-server && mvn clean compile -q

echo "Compilando client..."
cd ../dueling-client && mvn clean compile -q

echo "Compilação concluída!"

# Iniciar o gateway em background
echo "Configurando variáveis de ambiente..."
export SERVER_HOST=localhost
export SERVER_PORT=8082
export GATEWAY_HOST=localhost
export GATEWAY_PORT=8080
export POSTGRES_HOST=localhost
export POSTGRES_PORT=5432
export POSTGRES_DB=dueling_db
export POSTGRES_USER=dueling_user
export POSTGRES_PASSWORD=dueling_pass
export REDIS_SENTINEL_MASTER=mymaster
export REDIS_SENTINEL_NODES=localhost:26379,localhost:26380,localhost:26381

echo "Iniciando gateway..."
cd ../dueling-gateway
GATEWAY_HOST=localhost GATEWAY_PORT=8080 SERVER_HOST=localhost SERVER_PORT=8082 nohup mvn spring-boot:run -q > gateway.log 2>&1 &
GATEWAY_PID=$!

# Aguardar um pouco para o gateway iniciar
sleep 10

# Iniciar o server em background com configuração de Redis Sentinel
echo "Iniciando server..."
cd ../dueling-server
SPRING_PROFILES_ACTIVE=server,default \
SERVER_PORT=8082 \
POSTGRES_HOST=localhost POSTGRES_PORT=5432 POSTGRES_DB=dueling_db POSTGRES_USER=dueling_user POSTGRES_PASSWORD=dueling_pass \
REDIS_SENTINEL_MASTER=mymaster REDIS_SENTINEL_NODES=localhost:26379,localhost:26380,localhost:26381 \
nohup mvn spring-boot:run -q > server.log 2>&1 &
SERVER_PID=$!

# Aguardar um pouco para o server iniciar
sleep 25

echo "Servidores iniciados. Gateway PID: $GATEWAY_PID, Server PID: $SERVER_PID"

echo "Pronto para testar! Execute o cliente com o seguinte comando em outro terminal:"
echo "cd /home/lucas/Documentos/dev/projects/dueling-protocol/dueling-client && mvn compile exec:java -Dexec.mainClass=\"client.GameClient\""

echo ""
echo "Para acompanhar os logs dos servidores:"
echo "Gateway: tail -f /home/lucas/Documentos/dev/projects/dueling-protocol/dueling-gateway/gateway.log"
echo "Server: tail -f /home/lucas/Documentos/dev/projects/dueling-protocol/dueling-server/server.log"

echo ""
echo "Para parar tudo, execute:"
echo "kill $GATEWAY_PID $SERVER_PID"
echo "docker stop redis-master redis-replica 2>/dev/null"