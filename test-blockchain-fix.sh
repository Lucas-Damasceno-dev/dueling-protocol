#!/bin/bash

echo "🔧 TESTANDO CORREÇÃO BLOCKCHAIN - Chain ID"
echo "================================================"
echo ""

echo "📦 Step 1: Rebuilding containers..."
cd /home/lucas/Documentos/dev/projects/dueling-protocol/docker
docker compose down
docker compose up -d --build

echo ""
echo "⏳ Step 2: Waiting for services to start..."
sleep 45

echo ""
echo "✅ Step 3: Services should be ready!"
echo ""
echo "📋 Para testar:"
echo "  1. Terminal 1: ./menu.sh → 10 (Cliente 1)"
echo "  2. Terminal 2: ./menu.sh → 10 (Cliente 2)"
echo "  3. Ambos: Opção 1 (setup character)"
echo "  4. Ambos: Opção 4 (comprar pack)"
echo "  5. Um propõe trade (opção 8), outro aceita"
echo "  6. Ambos: Opção 2 (matchmaking) e jogar"
echo "  7. Verificar: ./menu.sh → 54"
echo ""
echo "🎯 RESULTADO ESPERADO:"
echo "  💳 CARDS MINTED: 10 cards ✅"
echo "  🔄 CARD TRANSFERS: 2 transfers ✅"
echo "  ⚔  MATCHES: 1 recorded ✅"
echo ""
