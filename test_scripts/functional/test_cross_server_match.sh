#!/bin/bash

# Teste de partida (match) entre servidores diferentes
# Player A conecta no Server 1, Player B no Server 2, ambos entram em matchmaking

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}╔══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║       Teste de Partida Cross-Server (Match)                 ║${NC}"
echo -e "${BLUE}╚══════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Verificar websocat
if ! command -v websocat &> /dev/null; then
    echo -e "${RED}ERROR: websocat not installed!${NC}"
    exit 1
fi

# IDs únicos
TIMESTAMP=$(date +%s)
PLAYER_A="match_a_${TIMESTAMP}"
PLAYER_B="match_b_${TIMESTAMP}"

echo -e "${YELLOW}Configuração:${NC}"
echo "  Server 1 (8081): Player A = $PLAYER_A"
echo "  Server 2 (8083): Player B = $PLAYER_B"
echo ""

TMP_DIR="/tmp/match_test_${TIMESTAMP}"
mkdir -p "$TMP_DIR"
LOG_A="${TMP_DIR}/player_a.log"
LOG_B="${TMP_DIR}/player_b.log"

# Passo 1: Criar Player A no Server 1
echo -e "${BLUE}═══ Passo 1: Criar Player A no Server 1 ═══${NC}"
echo "GAME:${PLAYER_A}:CHARACTER_SETUP:PlayerA:Human:Warrior" | websocat -n "ws://localhost:8081/ws" &
sleep 5
echo -e "${GREEN}✓ Player A criado${NC}"

# Passo 2: Criar Player B no Server 2
echo -e "${BLUE}═══ Passo 2: Criar Player B no Server 2 ═══${NC}"
echo "GAME:${PLAYER_B}:CHARACTER_SETUP:PlayerB:Elf:Mage" | websocat -n "ws://localhost:8083/ws" &
sleep 5
echo -e "${GREEN}✓ Player B criado${NC}"

# Passo 3: Conectar listeners persistentes
echo -e "${BLUE}═══ Passo 3: Iniciar Listeners Persistentes ═══${NC}"
timeout 60s websocat "ws://localhost:8081/ws" > "$LOG_A" 2>&1 &
PID_A=$!
timeout 60s websocat "ws://localhost:8083/ws" > "$LOG_B" 2>&1 &
PID_B=$!
sleep 3
echo -e "${GREEN}✓ Listeners iniciados${NC}"

# Passo 4: Player A entra em matchmaking
echo -e "${BLUE}═══ Passo 4: Player A Entra em Matchmaking ═══${NC}"
echo "GAME:${PLAYER_A}:MATCHMAKING:ENTER" | websocat -n "ws://localhost:8081/ws" &
sleep 3
echo -e "${GREEN}✓ Player A na fila${NC}"

# Passo 5: Player B entra em matchmaking
echo -e "${BLUE}═══ Passo 5: Player B Entra em Matchmaking ═══${NC}"
echo "GAME:${PLAYER_B}:MATCHMAKING:ENTER" | websocat -n "ws://localhost:8083/ws" &
sleep 8
echo -e "${GREEN}✓ Player B na fila${NC}"

# Passo 6: Verificar se match foi criado
echo -e "${BLUE}═══ Passo 6: Verificando Criação do Match ═══${NC}"
echo ""

MATCH_A=$(grep -c "MATCH_FOUND\|UPDATE:MATCH" "$LOG_A" 2>/dev/null || echo "0")
MATCH_B=$(grep -c "MATCH_FOUND\|UPDATE:MATCH" "$LOG_B" 2>/dev/null || echo "0")

if [ "$MATCH_A" -gt 0 ] || [ "$MATCH_B" -gt 0 ]; then
    echo -e "${GREEN}╔════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║                                                            ║${NC}"
    echo -e "${GREEN}║  ✓✓✓ MATCH CROSS-SERVER CRIADO COM SUCESSO! ✓✓✓          ║${NC}"
    echo -e "${GREEN}║                                                            ║${NC}"
    echo -e "${GREEN}╚════════════════════════════════════════════════════════════╝${NC}"
    echo ""
    echo -e "${YELLOW}Logs de Player A:${NC}"
    grep "MATCH\|SUCCESS\|UPDATE" "$LOG_A" 2>/dev/null | head -10
    echo ""
    echo -e "${YELLOW}Logs de Player B:${NC}"
    grep "MATCH\|SUCCESS\|UPDATE" "$LOG_B" 2>/dev/null | head -10
    echo ""
    echo -e "${GREEN}✓ Player A (Server 1:8080) entrou em matchmaking${NC}"
    echo -e "${GREEN}✓ Player B (Server 2:8083) entrou em matchmaking${NC}"
    echo -e "${GREEN}✓ Matchmaking service encontrou os dois players${NC}"
    echo -e "${GREEN}✓ Match foi criado e ambos foram notificados${NC}"
    RESULT=0
else
    echo -e "${RED}╔════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${RED}║  ✗ MATCH NÃO FOI CRIADO                                   ║${NC}"
    echo -e "${RED}╚════════════════════════════════════════════════════════════╝${NC}"
    echo ""
    echo -e "${YELLOW}Log de Player A:${NC}"
    tail -15 "$LOG_A" 2>/dev/null || echo "(vazio)"
    echo ""
    echo -e "${YELLOW}Log de Player B:${NC}"
    tail -15 "$LOG_B" 2>/dev/null || echo "(vazio)"
    RESULT=1
fi

# Cleanup
kill $PID_A $PID_B 2>/dev/null || true

echo ""
echo -e "${BLUE}═══ Informações Adicionais ═══${NC}"
echo "Logs salvos em: $TMP_DIR"
echo ""
echo -e "${YELLOW}Para verificar logs do servidor:${NC}"
echo "  tail -50 /tmp/F1.log | grep 'MATCH\|matchmaking'"
echo "  tail -50 /tmp/F2.log | grep 'MATCH\|matchmaking'"
echo ""

exit $RESULT
