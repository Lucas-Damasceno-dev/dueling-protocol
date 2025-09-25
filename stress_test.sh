#!/usr/bin/env bash

CLIENT_COUNT=10
TEST_DURATION=30 # in seconds

echo ">>> [PASSO 1/3] Garantindo que o projeto está compilado..."
./build.sh

echo ""
echo ">>> [PASSO 2/3] Construindo as imagens Docker..."
docker-compose build

echo ""
echo ">>> [PASSO 3/3] Iniciando o teste de estresse com 1 servidor e $CLIENT_COUNT clientes..."
echo ">>> O teste será executado por $TEST_DURATION segundos."
BOT_MODE=autobot docker-compose up --scale client=$CLIENT_COUNT --remove-orphans &

# Salva o PID do processo do docker-compose
COMPOSE_PID=$!

# Aguarda a duração do teste
sleep $TEST_DURATION

# Encerra o docker-compose
kill $COMPOSE_PID

echo ""
echo ">>> Teste encerrado. Limpando os contêineres..."
docker-compose down
