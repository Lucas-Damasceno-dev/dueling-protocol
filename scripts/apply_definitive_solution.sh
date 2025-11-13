#!/bin/bash

# Script para aplicar solução definitiva ao docker-compose.yml
# Modifica arquivo para deploy automático e leitura dinâmica de endereços

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
COMPOSE_FILE="$PROJECT_DIR/docker/docker-compose.yml"
BACKUP_FILE="$PROJECT_DIR/docker/docker-compose.yml.backup_$(date +%Y%m%d_%H%M%S)"

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║     APLICANDO SOLUÇÃO DEFINITIVA - Blockchain Automático      ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

# 1. Backup
echo "1️⃣  Criando backup do docker-compose.yml..."
cp "$COMPOSE_FILE" "$BACKUP_FILE"
echo "✅ Backup criado em: $BACKUP_FILE"
echo ""

# 2. Modificar blockchain para deploy automático
echo "2️⃣  Configurando deploy automático do blockchain..."
sed -i '/dueling-blockchain:/,/healthcheck:/ {
  /volumes:/,/networks:/ {
    s|blockchain-data:/usr/src/app/cache|blockchain-data:/usr/src/app/cache\n      - blockchain-deployments:/usr/src/app/deployments\n      - shared-blockchain:/usr/src/app/shared|
  }
}' "$COMPOSE_FILE"

# Adicionar command com deploy automático
sed -i '/dueling-blockchain:/,/healthcheck:/ {
  /volumes:/a\    command: sh -c "npx hardhat node --hostname 0.0.0.0 & sleep 10 && npx hardhat run scripts/deploy.js --network localhost && cp deployment-info.json shared/ && wait"
}' "$COMPOSE_FILE"

# Atualizar start_period do healthcheck
sed -i 's/start_period: 30s/start_period: 40s/g' "$COMPOSE_FILE"

echo "✅ Blockchain configurado para deploy automático"
echo ""

# 3. Adicionar volumes aos servidores
echo "3️⃣  Adicionando volumes compartilhados aos servidores..."
for server in server-1 server-2 server-3 server-4; do
  sed -i "/${server}:/,/healthcheck:/ {
    /environment:/i\    volumes:\n      - shared-blockchain:/shared:ro
  }" "$COMPOSE_FILE"
done
echo "✅ Volumes compartilhados adicionados"
echo ""

# 4. Remover env vars hardcoded e adicionar BLOCKCHAIN_DEPLOYMENT_FILE
echo "4️⃣  Removendo endereços hardcoded e adicionando arquivo de deployment..."
sed -i '/ASSET_CONTRACT:/d; /STORE_CONTRACT:/d; /TRADE_CONTRACT:/d; /MATCH_CONTRACT:/d' "$COMPOSE_FILE"
sed -i '/BLOCKCHAIN_NODE_URL:/a\      BLOCKCHAIN_DEPLOYMENT_FILE: "/shared/deployment-info.json"' "$COMPOSE_FILE"
echo "✅ Configuração dinâmica de endereços aplicada"
echo ""

# 5. Adicionar novos volumes
echo "5️⃣  Adicionando volumes persistentes..."
sed -i '/^volumes:/a\  blockchain-deployments:\n  shared-blockchain:' "$COMPOSE_FILE"
echo "✅ Volumes adicionados"
echo ""

# 6. Validar
echo "6️⃣  Validando docker-compose.yml..."
if docker compose -f "$COMPOSE_FILE" config > /dev/null 2>&1; then
    echo "✅ docker-compose.yml válido!"
else
    echo "❌ Erro no docker-compose.yml!"
    echo "   Restaurando backup..."
    cp "$BACKUP_FILE" "$COMPOSE_FILE"
    echo "   Backup restaurado"
    exit 1
fi
echo ""

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║              ✅ SOLUÇÃO DEFINITIVA APLICADA!                   ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""
echo "Próximos passos:"
echo "  1. cd docker"
echo "  2. docker compose build"
echo "  3. docker compose down && docker compose up -d"
echo "  4. Aguardar ~40 segundos (deploy automático)"
echo "  5. Testar!"
echo ""
echo "Para reverter: cp $BACKUP_FILE $COMPOSE_FILE"
echo ""
