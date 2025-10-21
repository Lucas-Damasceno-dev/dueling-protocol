#!/bin/bash

# Script para resetar o estoque de cartas no Redis

echo "=== Resetando Estoque de Cartas ==="
echo

# Verificar se o Redis está rodando
if ! docker ps | grep -q "redis-master"; then
    echo "❌ Redis não está rodando!"
    exit 1
fi

echo "🔄 Deletando chave card:stock do Redis..."
docker exec redis-master redis-cli DEL "card:stock"

echo "✅ Estoque deletado!"
echo
echo "🔄 Reiniciando servidor para recarregar o estoque..."
docker restart server-1

echo
echo "⏳ Aguardando servidor inicializar (30 segundos)..."
sleep 30

echo
echo "📊 Verificando estoque no Redis:"
docker exec redis-master redis-cli HLEN "card:stock"
echo "   ^ Deve ser 15"

echo
echo "✅ Pronto! Tente comprar cartas novamente."
echo "   Para ver os logs: docker logs -f server-1"
