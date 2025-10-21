#!/bin/bash

# Teste simplificado de troca cross-server SEM autenticação JWT
# Usa WebSocket direto para criar players e testar troca

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}╔══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║       Teste Simplificado de Troca Cross-Server              ║${NC}"
echo -e "${BLUE}╚══════════════════════════════════════════════════════════════╝${NC}"
echo ""

# IDs únicos
TIMESTAMP=$(date +%s)
PLAYER_A="player_a_${TIMESTAMP}"
PLAYER_B="player_b_${TIMESTAMP}"

echo -e "${YELLOW}Configuração:${NC}"
echo "  Server 1 (8080): Player A = $PLAYER_A"
echo "  Server 2 (8083): Player B = $PLAYER_B"
echo ""

# Verificar se websocat está instalado
if ! command -v websocat &> /dev/null; then
    echo -e "${RED}ERRO: websocat não está instalado!${NC}"
    echo "Instale com: cargo install websocat"
    echo "Ou: sudo apt-get install websocat"
    exit 1
fi

# Criar diretórios temporários para logs
TMP_DIR="/tmp/trade_test_${TIMESTAMP}"
mkdir -p "$TMP_DIR"
LOG_A="${TMP_DIR}/player_a.log"
LOG_B="${TMP_DIR}/player_b.log"

echo -e "${BLUE}═══ Passo 1: Criar Player A no Server 1 ═══${NC}"
echo "GAME:${PLAYER_A}:CHARACTER_SETUP:Nick_A:Human:Warrior" | websocat -n "ws://localhost:8080/ws" > "$LOG_A" 2>&1 &
sleep 3

if grep -q "SUCCESS:Character created" "$LOG_A"; then
    echo -e "${GREEN}✓ Player A criado com sucesso${NC}"
else
    echo -e "${RED}✗ Falha ao criar Player A${NC}"
    cat "$LOG_A"
    exit 1
fi

echo -e "${BLUE}═══ Passo 2: Criar Player B no Server 2 ═══${NC}"
echo "GAME:${PLAYER_B}:CHARACTER_SETUP:Nick_B:Elf:Mage" | websocat -n "ws://localhost:8083/ws" > "$LOG_B" 2>&1 &
sleep 3

if grep -q "SUCCESS:Character created" "$LOG_B"; then
    echo -e "${GREEN}✓ Player B criado com sucesso${NC}"
else
    echo -e "${RED}✗ Falha ao criar Player B${NC}"
    cat "$LOG_B"
    exit 1
fi

echo -e "${BLUE}═══ Passo 3: Iniciar Listeners ═══${NC}"
# Iniciar listener persistente para Player B (vai receber a proposta)
websocat "ws://localhost:8083/ws" > "$LOG_B" 2>&1 &
PID_B=$!
sleep 2

# Enviar proposta de Player A para Player B
echo -e "${BLUE}═══ Passo 4: Player A Propõe Troca ═══${NC}"
echo -e "${YELLOW}Enviando: GAME:${PLAYER_A}:TRADE:PROPOSE:${PLAYER_B}:basic-0:basic-1${NC}"

# Criar comando para Player A
TRADE_PROPOSAL="GAME:${PLAYER_A}:TRADE:PROPOSE:${PLAYER_B}:basic-0:basic-1"
echo "$TRADE_PROPOSAL" | websocat -n "ws://localhost:8080/ws" > "${TMP_DIR}/proposal_response.log" 2>&1 &
sleep 5

# Verificar se Player B recebeu a proposta
echo -e "${BLUE}═══ Passo 5: Verificar Recebimento da Proposta ═══${NC}"

if grep -q "UPDATE:TRADE_PROPOSAL" "$LOG_B"; then
    echo -e "${GREEN}✓ Player B recebeu a proposta!${NC}"
    
    # Extrair Trade ID
    TRADE_ID=$(grep "UPDATE:TRADE_PROPOSAL" "$LOG_B" | tail -1 | cut -d':' -f3)
    echo -e "${GREEN}Trade ID extraído: $TRADE_ID${NC}"
    
    if [ -z "$TRADE_ID" ]; then
        echo -e "${RED}✗ Não foi possível extrair Trade ID${NC}"
        cat "$LOG_B"
        kill $PID_B 2>/dev/null || true
        exit 1
    fi
    
    # Player B aceita a troca
    echo -e "${BLUE}═══ Passo 6: Player B Aceita a Troca ═══${NC}"
    echo -e "${YELLOW}Enviando: GAME:${PLAYER_B}:TRADE:ACCEPT:${TRADE_ID}${NC}"
    
    # Parar listener anterior
    kill $PID_B 2>/dev/null || true
    sleep 1
    
    # Reiniciar listeners para ambos
    websocat "ws://localhost:8080/ws" > "${TMP_DIR}/player_a_accept.log" 2>&1 &
    PID_A=$!
    websocat "ws://localhost:8083/ws" > "${TMP_DIR}/player_b_accept.log" 2>&1 &
    PID_B=$!
    sleep 2
    
    # Enviar aceitação
    TRADE_ACCEPT="GAME:${PLAYER_B}:TRADE:ACCEPT:${TRADE_ID}"
    echo "$TRADE_ACCEPT" | websocat -n "ws://localhost:8083/ws" 2>&1 | tee -a "${TMP_DIR}/accept_response.log" &
    sleep 5
    
    # Verificar resultado
    echo -e "${BLUE}═══ Passo 7: Verificar Resultado ═══${NC}"
    
    SUCCESS_A=$(grep -c "TRADE_COMPLETE:SUCCESS" "${TMP_DIR}/player_a_accept.log" 2>/dev/null || echo "0")
    SUCCESS_B=$(grep -c "TRADE_COMPLETE:SUCCESS" "${TMP_DIR}/player_b_accept.log" 2>/dev/null || echo "0")
    
    echo ""
    echo -e "${YELLOW}Logs de Player A:${NC}"
    cat "${TMP_DIR}/player_a_accept.log" 2>/dev/null | tail -10 || echo "Sem logs"
    echo ""
    echo -e "${YELLOW}Logs de Player B:${NC}"
    cat "${TMP_DIR}/player_b_accept.log" 2>/dev/null | tail -10 || echo "Sem logs"
    echo ""
    
    if [ "$SUCCESS_A" -gt 0 ] && [ "$SUCCESS_B" -gt 0 ]; then
        echo -e "${GREEN}╔════════════════════════════════════════════════════════════╗${NC}"
        echo -e "${GREEN}║                                                            ║${NC}"
        echo -e "${GREEN}║  ✓✓✓ TROCA CROSS-SERVER EXECUTADA COM SUCESSO! ✓✓✓       ║${NC}"
        echo -e "${GREEN}║                                                            ║${NC}"
        echo -e "${GREEN}╚════════════════════════════════════════════════════════════╝${NC}"
        RESULT=0
    else
        echo -e "${RED}╔════════════════════════════════════════════════════════════╗${NC}"
        echo -e "${RED}║                                                            ║${NC}"
        echo -e "${RED}║  ✗ TROCA FALHOU - Verifique os logs                       ║${NC}"
        echo -e "${RED}║                                                            ║${NC}"
        echo -e "${RED}╚════════════════════════════════════════════════════════════╝${NC}"
        RESULT=1
    fi
    
    # Cleanup
    kill $PID_A $PID_B 2>/dev/null || true
    
else
    echo -e "${RED}✗ Player B NÃO recebeu a proposta de troca${NC}"
    echo -e "${YELLOW}Log de Player B:${NC}"
    cat "$LOG_B"
    kill $PID_B 2>/dev/null || true
    RESULT=1
fi

echo ""
echo -e "${BLUE}═══ Logs Salvos Em ═══${NC}"
echo "  Diretório: $TMP_DIR"
ls -lh "$TMP_DIR"

echo ""
echo -e "${YELLOW}Para ver logs do servidor:${NC}"
echo "  tail -100 dueling-server/logs/application.log | grep '\\[TRADE'"

exit $RESULT
