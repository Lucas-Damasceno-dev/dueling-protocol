#!/bin/bash
set -e

echo "Aguardando servidores estarem prontos..."

# Aguardar até que pelo menos um servidor esteja saudável
until curl -f http://server-1:8080/actuator/health >/dev/null 2>&1 || \
      curl -f http://server-2:8080/actuator/health >/dev/null 2>&1 || \
      curl -f http://server-3:8080/actuator/health >/dev/null 2>&1 || \
      curl -f http://server-4:8080/actuator/health >/dev/null 2>&1; do
    echo "Servidores ainda não estão prontos, aguardando..."
    sleep 2
done

echo "Pelo menos um servidor está pronto, iniciando NGINX..."
exec "$@"