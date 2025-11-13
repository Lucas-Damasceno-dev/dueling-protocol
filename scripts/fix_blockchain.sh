#!/bin/bash

# Script para corrigir blockchain e fazer sistema funcionar
# Execute este script TODA VEZ que reiniciar o sistema

cd "$(dirname "$0")/.."

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║     🔧 FIX BLOCKCHAIN - Solução Temporária Automática         ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

# 1. Verificar se blockchain está rodando
echo "1️⃣  Verificando se blockchain está rodando..."
if ! docker ps | grep -q "dueling-blockchain"; then
    echo "❌ Blockchain não está rodando!"
    echo "   Execute: ./menu.sh → opção 5"
    exit 1
fi
echo "✅ Blockchain está rodando"
echo ""

# 2. Aguardar blockchain estar pronto
echo "2️⃣  Aguardando blockchain ficar pronto..."
sleep 5

# 3. Fazer deploy dos contratos
echo "3️⃣  Fazendo deploy dos contratos..."
cd dueling-blockchain
npx hardhat run scripts/deploy.js --network localhost > /tmp/deploy.log 2>&1

if [ $? -ne 0 ]; then
    echo "❌ Deploy falhou!"
    cat /tmp/deploy.log
    exit 1
fi

echo "✅ Deploy concluído"
echo ""

# 4. Atualizar endereços em todos os arquivos
echo "4️⃣  Atualizando endereços dos contratos..."
cd ..
bash scripts/update_blockchain_addresses.sh | grep -E "AssetContract|StoreContract|TradeContract|MatchContract"
echo ""

# 5. Recriar servidores para carregar novas variáveis de ambiente
echo "5️⃣  Recriando servidores com novos endereços..."
docker compose -f docker/docker-compose.yml up -d --force-recreate --no-deps server-1 server-2 server-3 server-4 nginx-gateway 2>&1 | grep -E "Started|Recreated"
echo "✅ Servidores recriados"
echo ""

# 6. Verificar se funcionou
echo "6️⃣  Verificando deployment..."
sleep 3
bash scripts/verify_blockchain_ledger.sh 2>&1 | grep -E "Contract Addresses|AssetContract|MatchContract"
echo ""

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║                    ✅ CORREÇÃO COMPLETA!                       ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""
echo "Agora você pode:"
echo "  1. Rodar clientes: ./menu.sh → opção 10"
echo "  2. Fazer: compras, trades, partidas"
echo "  3. Verificar ledger: ./menu.sh → opção 41"
echo ""
echo "⚠️  LEMBRE-SE: Este script precisa rodar TODA VEZ que reiniciar o sistema!"
echo ""
