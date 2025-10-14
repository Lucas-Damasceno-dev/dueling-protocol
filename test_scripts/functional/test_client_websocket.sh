#!/bin/bash

# Script para testar o cliente de forma automatizada
echo "=== Teste Automatizado do Cliente do Protocolo de Duelo ==="

# Registrar um usuário de teste
echo "Registrando usuário de teste..."
# Gerar um nome de usuário único baseado no timestamp
TIMESTAMP=$(date +%s)
TEST_USERNAME="testuser_$TIMESTAMP"
curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$TEST_USERNAME\",\"password\":\"password123456789\"}" > /tmp/register_response.json

# Verificar se o registro foi bem-sucedido
if grep -q '"error":null' /tmp/register_response.json; then
    echo "[SUCCESS] ✓ Registro bem-sucedido"
else
    echo "[ERROR] ✗ Falha no registro"
    cat /tmp/register_response.json
    exit 1
fi

# Fazer login com o usuário de teste
echo "Fazendo login..."
LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$TEST_USERNAME\",\"password\":\"password123456789\"}")

# Extrair o token JWT
JWT_TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

if [ -z "$JWT_TOKEN" ]; then
    echo "[ERROR] ✗ Falha ao obter token JWT"
    echo "$LOGIN_RESPONSE"
    exit 1
else
    echo "[SUCCESS] ✓ Login bem-sucedido"
    echo "Token JWT obtido: ${JWT_TOKEN:0:20}..."
fi

# Testar conexão WebSocket com o token
echo "Testando conexão WebSocket..."

# Codificar o token para URL
ENCODED_TOKEN=$(echo "$JWT_TOKEN" | sed 's/+/%2B/g; s/\//%2F/g; s/=/%3D/g')

# Tentar conexão WebSocket
WEBSOCKET_URL="ws://localhost:8080/ws?token=$ENCODED_TOKEN"

echo "URL WebSocket: $WEBSOCKET_URL"

# Usar wscat para testar a conexão WebSocket (se disponível)
if command -v wscat &> /dev/null; then
    echo "Usando wscat para testar conexão WebSocket..."
    
    # Testar conexão básica primeiro
    echo "Testando conexão WebSocket com timeout..."
    timeout 10 bash -c '
    {
        sleep 1
        echo "PING"
        sleep 2
        echo "quit"
    } | wscat -c "'"$WEBSOCKET_URL"'" 2>&1 | tee /tmp/wscat_output.txt
    ' || echo "Tempo limite atingido ou falha na conexão"
    
    # Mostrar output
    if [ -f /tmp/wscat_output.txt ]; then
        echo "Saída do wscat:"
        cat /tmp/wscat_output.txt
        rm -f /tmp/wscat_output.txt
    fi
    
else
    echo "wscat não disponível. Usando curl para verificar se o endpoint WebSocket responde..."
    # Testar se o endpoint WebSocket responde com status 101 (Switching Protocols)
    curl -v -X GET "$WEBSOCKET_URL" \
      -H "Connection: Upgrade" \
      -H "Upgrade: websocket" \
      -H "Sec-WebSocket-Version: 13" \
      -H "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==" \
      2>&1 | grep -E "(101 Switching Protocols|401 Unauthorized|403 Forbidden|200 OK)" || echo "Nenhuma resposta relevante encontrada"
fi

# Limpar arquivos temporários
rm -f /tmp/register_response.json

echo "=== Fim do teste automatizado ==="