#!/bin/bash
# Script para registrar os usuários dos clientes

# Aguardar os serviços estarem prontos
sleep 10

echo "Registrando usuários..."

# Registrar os usuários dos clientes
for i in $(seq 1 4); do
  echo "Registrando client$i..."
  curl -X POST http://localhost:808$i/api/auth/register \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"client$i\",\"password\":\"password$i\",\"playerId\":\"player$i\"}"
    
  if [ $? -eq 0 ]; then
    echo "client$i registrado com sucesso"
  else
    echo "Erro ao registrar client$i"
  fi
done

echo "Todos os usuários registrados!"