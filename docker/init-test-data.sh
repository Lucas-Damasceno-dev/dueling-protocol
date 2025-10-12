#!/bin/bash
# Script para inicializar dados de teste no banco de dados

# Esperar até que o banco de dados esteja pronto
echo "Aguardando o banco de dados ficar pronto..."
until pg_isready -h postgres -U ${POSTGRES_USER} -d ${POSTGRES_DB}; do
  echo "Banco de dados ainda não está pronto, aguardando..."
  sleep 2
done

echo "Banco de dados está pronto!"

# Executar o script de inicialização de dados de teste
echo "Inicializando dados de teste..."
psql -h postgres -U ${POSTGRES_USER} -d ${POSTGRES_DB} -f /init-test-data.sql

echo "Dados de teste inicializados!"