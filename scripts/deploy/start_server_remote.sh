#!/bin/bash
# Script para iniciar servidor em máquina remota
echo "=== Iniciar Servidor Remoto ==="

# Compilar se necessário
if [ ! -f "dueling-server/target/dueling-server-1.0-SNAPSHOT.jar" ]; then
    echo "Compilando servidor..."
    mvn clean package -DskipTests
fi

read -p "IP do PostgreSQL: " POSTGRES_HOST
read -p "Porta do PostgreSQL (padrão 5432): " -e -i "5432" POSTGRES_PORT
read -p "IP do Redis Sentinel (padrão localhost): " -e -i "localhost" REDIS_HOST
read -p "Porta do servidor (padrão 8083): " -e -i "8083" SERVER_PORT

cd dueling-server
java -DSERVER_PORT=$SERVER_PORT \
     -DPOSTGRES_HOST=$POSTGRES_HOST \
     -DPOSTGRES_PORT=$POSTGRES_PORT \
     -DPOSTGRES_DB=dueling_db \
     -DPOSTGRES_USER=dueling_user \
     -DPOSTGRES_PASSWORD=dueling_pass \
     -DREDIS_SENTINEL_MASTER=mymaster \
     -DREDIS_SENTINEL_NODES=$REDIS_HOST:26379,$REDIS_HOST:26380,$REDIS_HOST:26381 \
     -jar target/dueling-server-1.0-SNAPSHOT.jar