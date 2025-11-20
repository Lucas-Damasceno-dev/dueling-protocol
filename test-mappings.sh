#!/bin/bash

echo "=== Teste de Mapeamentos ==="
echo ""
echo "Diretório atual: $(pwd)"
echo ""

echo "1. Verificando arquivos de mapeamento:"
if [ -f "dueling-blockchain/address-mapping.json" ]; then
    echo "   ✅ address-mapping.json encontrado"
    echo "   Conteúdo:"
    cat dueling-blockchain/address-mapping.json | jq '.' 2>/dev/null || cat dueling-blockchain/address-mapping.json
else
    echo "   ❌ address-mapping.json NÃO encontrado"
fi

echo ""

if [ -f "dueling-blockchain/card-token-mapping.json" ]; then
    echo "   ✅ card-token-mapping.json encontrado"
    echo "   Conteúdo (primeiras 10 entradas):"
    cat dueling-blockchain/card-token-mapping.json | jq '.' 2>/dev/null | head -20 || cat dueling-blockchain/card-token-mapping.json | head -20
else
    echo "   ❌ card-token-mapping.json NÃO encontrado"
fi

echo ""
echo "2. Verificando diretório dueling-blockchain:"
ls -la dueling-blockchain/*.json 2>/dev/null || echo "   Nenhum arquivo .json encontrado"

echo ""
echo "=== Fim do Teste ==="
