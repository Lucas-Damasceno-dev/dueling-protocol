#!/bin/bash

# Teste de troca cross-server COM autenticação JWT completa

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}╔══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║       Teste de Troca Cross-Server (COM Autenticação)        ║${NC}"
echo -e "${BLUE}╚══════════════════════════════════════════════════════════════╝${NC}"
echo ""

# IDs únicos
TIMESTAMP=$(date +%s)
PLAYER_A="player_a_${TIMESTAMP}"
PLAYER_B="player_b_${TIMESTAMP}"
PASS="testpass123"

echo -e "${YELLOW}Configuração:${NC}"
echo "  Server 1 (8080): Player A = $PLAYER_A"
echo "  Server 2 (8083): Player B = $PLAYER_B"
echo ""

TMP_DIR="/tmp/trade_auth_test_${TIMESTAMP}"
mkdir -p "$TMP_DIR"

# Passo 1: Criar Player A via WebSocket SEM auth no Server 1
echo -e "${BLUE}═══ Passo 1: Criar Player A no Server 1 via WS ═══${NC}"
echo "GAME:${PLAYER_A}:CHARACTER_SETUP:Nick_A:Human:Warrior" | timeout 5s websocat -n "ws://localhost:8080/ws" > "${TMP_DIR}/char_a.log" 2>&1 || true

# Verificar se foi criado (mesmo com erro de auth, o comando pode ter sido processado)
sleep 2

# Passo 2: Criar Player B via WebSocket SEM auth no Server 2  
echo -e "${BLUE}═══ Passo 2: Criar Player B no Server 2 via WS ═══${NC}"
echo "GAME:${PLAYER_B}:CHARACTER_SETUP:Nick_B:Elf:Mage" | timeout 5s websocat -n "ws://localhost:8083/ws" > "${TMP_DIR}/char_b.log" 2>&1 || true

sleep 2

# Passo 3: Registrar usuários (agora que os players existem)
echo -e "${BLUE}═══ Passo 3: Registrar Usuários com Senhas ═══${NC}"

REGISTER_A=$(curl -s -X POST "http://localhost:8080/api/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"user_${PLAYER_A}\",\"password\":\"${PASS}\",\"playerId\":\"${PLAYER_A}\"}")

echo "Registro A: $REGISTER_A"

REGISTER_B=$(curl -s -X POST "http://localhost:8083/api/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"user_${PLAYER_B}\",\"password\":\"${PASS}\",\"playerId\":\"${PLAYER_B}\"}")

echo "Registro B: $REGISTER_B"

# Passo 4: Fazer login e obter tokens
echo -e "${BLUE}═══ Passo 4: Fazer Login e Obter Tokens JWT ═══${NC}"

TOKEN_A=$(curl -s -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"user_${PLAYER_A}\",\"password\":\"${PASS}\"}" | jq -r '.token // empty')

if [ -z "$TOKEN_A" ]; then
    echo -e "${RED}✗ Falha ao obter token para Player A${NC}"
    echo "Response: $(curl -s -X POST "http://localhost:8080/api/auth/login" -H "Content-Type: application/json" -d "{\"username\":\"user_${PLAYER_A}\",\"password\":\"${PASS}\"}")"
    exit 1
fi

TOKEN_B=$(curl -s -X POST "http://localhost:8083/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"user_${PLAYER_B}\",\"password\":\"${PASS}\"}" | jq -r '.token // empty')

if [ -z "$TOKEN_B" ]; then
    echo -e "${RED}✗ Falha ao obter token para Player B${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Tokens JWT obtidos com sucesso${NC}"
echo "Token A: ${TOKEN_A:0:20}..."
echo "Token B: ${TOKEN_B:0:20}..."

# Passo 5: Conectar WebSockets autenticados
echo -e "${BLUE}═══ Passo 5: Conectar WebSockets Autenticados ═══${NC}"

# Iniciar listener para Player B (autenticado)
websocat "ws://localhost:8083/ws?token=${TOKEN_B}" > "${TMP_DIR}/player_b.log" 2>&1 &
PID_B=$!
sleep 2

# Passo 6: Player A propõe troca
echo -e "${BLUE}═══ Passo 6: Player A Propõe Troca para Player B ═══${NC}"
TRADE_CMD="GAME:${PLAYER_A}:TRADE:PROPOSE:${PLAYER_B}:basic-0:basic-1"
echo -e "${YELLOW}Comando: $TRADE_CMD${NC}"

echo "$TRADE_CMD" | websocat -n "ws://localhost:8080/ws?token=${TOKEN_A}" > "${TMP_DIR}/proposal.log" 2>&1 &

sleep 7

# Verificar se Player B recebeu
echo -e "${BLUE}═══ Passo 7: Verificar se Player B Recebeu a Proposta ═══${NC}"

if grep -q "UPDATE:TRADE_PROPOSAL" "${TMP_DIR}/player_b.log"; then
    echo -e "${GREEN}✓ Player B recebeu a proposta via Redis Pub/Sub!${NC}"
    
    TRADE_ID=$(grep "UPDATE:TRADE_PROPOSAL" "${TMP_DIR}/player_b.log" | tail -1 | cut -d':' -f3)
    echo -e "${GREEN}Trade ID: $TRADE_ID${NC}"
    
    if [ -n "$TRADE_ID" ]; then
        # Parar listener
        kill $PID_B 2>/dev/null || true
        sleep 1
        
        # Passo 8: Iniciar listeners para ambos
        echo -e "${BLUE}═══ Passo 8: Player B Aceita a Troca ═══${NC}"
        
        websocat "ws://localhost:8080/ws?token=${TOKEN_A}" > "${TMP_DIR}/player_a_final.log" 2>&1 &
        PID_A=$!
        websocat "ws://localhost:8083/ws?token=${TOKEN_B}" > "${TMP_DIR}/player_b_final.log" 2>&1 &
        PID_B=$!
        sleep 2
        
        # Aceitar troca
        ACCEPT_CMD="GAME:${PLAYER_B}:TRADE:ACCEPT:${TRADE_ID}"
        echo -e "${YELLOW}Comando: $ACCEPT_CMD${NC}"
        echo "$ACCEPT_CMD" | websocat -n "ws://localhost:8083/ws?token=${TOKEN_B}" 2>&1 | tee "${TMP_DIR}/accept.log" &
        
        sleep 8
        
        # Verificar resultado
        echo -e "${BLUE}═══ Passo 9: Verificar Resultado Final ═══${NC}"
        
        echo ""
        echo -e "${YELLOW}Logs de Player A:${NC}"
        cat "${TMP_DIR}/player_a_final.log" 2>/dev/null | tail -15
        echo ""
        echo -e "${YELLOW}Logs de Player B:${NC}"
        cat "${TMP_DIR}/player_b_final.log" 2>/dev/null | tail -15
        echo ""
        
        SUCCESS_A=$(grep -c "TRADE_COMPLETE:SUCCESS" "${TMP_DIR}/player_a_final.log" 2>/dev/null || echo "0")
        SUCCESS_B=$(grep -c "TRADE_COMPLETE:SUCCESS" "${TMP_DIR}/player_b_final.log" 2>/dev/null || echo "0")
        
        if [ "$SUCCESS_A" -gt 0 ] && [ "$SUCCESS_B" -gt 0 ]; then
            echo -e "${GREEN}╔════════════════════════════════════════════════════════════╗${NC}"
            echo -e "${GREEN}║                                                            ║${NC}"
            echo -e "${GREEN}║  ✓✓✓ TROCA CROSS-SERVER BEM-SUCEDIDA! ✓✓✓                ║${NC}"
            echo -e "${GREEN}║                                                            ║${NC}"
            echo -e "${GREEN}║  • Player A (Server 1:8080) propôs troca                  ║${NC}"
            echo -e "${GREEN}║  • Player B (Server 2:8083) recebeu via Redis Pub/Sub     ║${NC}"
            echo -e "${GREEN}║  • Player B aceitou a troca                                ║${NC}"
            echo -e "${GREEN}║  • Ambos receberam confirmação de sucesso                  ║${NC}"
            echo -e "${GREEN}║                                                            ║${NC}"
            echo -e "${GREEN}╚════════════════════════════════════════════════════════════╝${NC}"
            RESULT=0
        else
            echo -e "${RED}╔════════════════════════════════════════════════════════════╗${NC}"
            echo -e "${RED}║  ✗ TROCA FALHOU - Verifique os logs                       ║${NC}"
            echo -e "${RED}╚════════════════════════════════════════════════════════════╝${NC}"
            RESULT=1
        fi
        
        kill $PID_A $PID_B 2>/dev/null || true
    else
        echo -e "${RED}✗ Não foi possível extrair Trade ID${NC}"
        RESULT=1
    fi
else
    echo -e "${RED}✗ Player B NÃO recebeu a proposta (falha no Redis Pub/Sub?)${NC}"
    echo ""
    echo -e "${YELLOW}Log de Player B:${NC}"
    cat "${TMP_DIR}/player_b.log"
    kill $PID_B 2>/dev/null || true
    RESULT=1
fi

echo ""
echo -e "${BLUE}═══ Logs de Servidor ═══${NC}"
echo "Verifique os logs detalhados com:"
echo "  tail -100 dueling-server/logs/application.log | grep '\\[TRADE'"
echo ""
echo "Logs do teste salvos em: $TMP_DIR"

exit $RESULT
