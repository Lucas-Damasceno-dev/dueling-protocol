#!/bin/bash
# Script para iniciar os clientes Docker após os servidores estarem prontos

echo "Esperando os servidores estarem prontos..."

# Esperar até que os servidores estejam prontos
MAX_ATTEMPTS=60
ATTEMPT=0

while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
  echo "Tentativa $((ATTEMPT+1))/$MAX_ATTEMPTS - Verificando servidores..."
  
  if curl -f http://server-1:8080/actuator/health >/dev/null 2>&1 && \
     curl -f http://server-2:8080/actuator/health >/dev/null 2>&1 && \
     curl -f http://server-3:8080/actuator/health >/dev/null 2>&1 && \
     curl -f http://server-4:8080/actuator/health >/dev/null 2>&1; then
    echo "Servidores prontos!"
    break
  fi
  
  ATTEMPT=$((ATTEMPT+1))
  sleep 5
done

if [ $ATTEMPT -ge $MAX_ATTEMPTS ]; then
  echo "Tempo limite excedido esperando pelos servidores."
  exit 1
fi

echo "Iniciando cliente Docker..."

# Iniciar o cliente com as variáveis de ambiente
echo "Iniciando client com GATEWAY_HOST=$GATEWAY_HOST e GATEWAY_PORT=$GATEWAY_PORT"
java -jar /app.jar