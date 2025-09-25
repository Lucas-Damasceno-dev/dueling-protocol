#!/bin/bash

CLIENT_COUNT=10

echo ">>> [PASSO 1/3] Garantindo que o projeto está compilado..."
./build.sh

echo ""
echo ">>> [PASSO 2/3] Construindo as imagens Docker..."
docker-compose build

echo ""
echo ">>> [PASSO 3/3] Iniciando o teste de estresse com 1 servidor e $CLIENT_COUNT clientes..."
echo ">>> Pressione Ctrl+C para encerrar o teste."
docker-compose up --scale client=$CLIENT_COUNT --remove-orphans

echo ""
echo ">>> Teste encerrado. Limpando os contêineres..."
docker-compose down