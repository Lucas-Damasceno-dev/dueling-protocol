#!/bin/bash

# ============================================
# TESTE FINAL: PURCHASE + TRADE + MATCHMAKING
# ============================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR/.."

echo "╔════════════════════════════════════════════════════╗"
echo "║  TESTE FINAL: PURCHASE + TRADE + MATCHMAKING      ║"
echo "╚════════════════════════════════════════════════════╝"

# Run Node.js test
cd "$SCRIPT_DIR"
node test_websocket_features.js

EXIT_CODE=$?

if [ $EXIT_CODE -eq 0 ]; then
    echo -e "\n🎉🎉🎉 TODOS OS TESTES PASSARAM! 🎉🎉🎉"
else
    echo -e "\n❌ Alguns testes falharam. Exit code: $EXIT_CODE"
    echo ""
    echo "Verificando detalhes nos logs do servidor..."
    docker logs server-1 2>&1 | grep -i "error\|trade\|match" | tail -20
fi

exit $EXIT_CODE
