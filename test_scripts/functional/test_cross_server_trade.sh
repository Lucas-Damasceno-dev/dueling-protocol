#!/bin/bash

# Script para testar troca de cartas entre servidores diferentes
# Este script simula dois jogadores conectados a servidores diferentes

set -e

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configurações dos servidores
SERVER1_URL="${SERVER1_URL:-http://localhost:8080}"
SERVER2_URL="${SERVER2_URL:-http://localhost:8083}"
WS1_URL="${WS1_URL:-ws://localhost:8080/ws}"
WS2_URL="${WS2_URL:-ws://localhost:8083/ws}"

# Nomes únicos de usuários
TIMESTAMP=$(date +%s)
USER_A="trader_a_${TIMESTAMP}"
USER_B="trader_b_${TIMESTAMP}"
PASS="password123"

echo -e "${BLUE}╔══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║       Teste de Troca de Cartas Cross-Server                 ║${NC}"
echo -e "${BLUE}╚══════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${YELLOW}Configuração:${NC}"
echo "  Server 1: $SERVER1_URL"
echo "  Server 2: $SERVER2_URL"
echo "  User A: $USER_A (conectado ao Server 1)"
echo "  User B: $USER_B (conectado ao Server 2)"
echo ""

# Função para registrar usuário
register_user() {
    local server_url=$1
    local username=$2
    local password=$3
    
    echo -e "${GREEN}[1/4] Registrando $username em $server_url...${NC}"
    response=$(curl -s -X POST "${server_url}/api/auth/register" \
        -H "Content-Type: application/json" \
        -d "{\"username\": \"$username\", \"password\": \"$password\", \"playerId\": \"$username\"}")
    
    if echo "$response" | grep -q "error\|Error"; then
        echo -e "${RED}Erro ao registrar $username: $response${NC}"
        return 1
    fi
    echo -e "${GREEN}✓ $username registrado com sucesso${NC}"
    return 0
}

# Função para fazer login
login_user() {
    local server_url=$1
    local username=$2
    local password=$3
    
    echo -e "${GREEN}[2/4] Fazendo login de $username...${NC}"
    response=$(curl -s -X POST "${server_url}/api/auth/login" \
        -H "Content-Type: application/json" \
        -d "{\"username\": \"$username\", \"password\": \"$password\"}")
    
    token=$(echo "$response" | jq -r '.token' 2>/dev/null)
    if [ -z "$token" ] || [ "$token" = "null" ]; then
        echo -e "${RED}Erro ao fazer login de $username: $response${NC}"
        return 1
    fi
    echo -e "${GREEN}✓ Login de $username bem-sucedido${NC}"
    echo "$token"
    return 0
}

# Função para criar personagem
create_character() {
    local ws_url=$1
    local token=$2
    local username=$3
    
    echo -e "${GREEN}[3/4] Criando personagem para $username...${NC}"
    
    # Criar script temporário para websocat
    local temp_script="/tmp/char_setup_${username}.txt"
    cat > "$temp_script" << EOF
GAME:${username}:CHARACTER_SETUP:${username}_nick:Human:Warrior
EOF
    
    timeout 5s websocat -n "${ws_url}?token=${token}" < "$temp_script" 2>&1 | head -5 &
    sleep 2
    rm -f "$temp_script"
    
    echo -e "${GREEN}✓ Personagem criado${NC}"
}

# Função para verificar cartas disponíveis
check_cards() {
    local username=$1
    local token=$2
    local server_url=$3
    
    echo -e "${YELLOW}Verificando cartas de $username...${NC}"
    
    # Tentar obter informações do jogador (assumindo que existe um endpoint)
    # Como não temos certeza do endpoint, vamos usar o websocket para enviar LIST_INVENTORY
    local temp_script="/tmp/list_cards_${username}.txt"
    cat > "$temp_script" << EOF
GAME:${username}:LIST_INVENTORY
EOF
    
    output=$(timeout 5s websocat -n "${WS1_URL}?token=${token}" < "$temp_script" 2>&1 | grep "UPDATE:INVENTORY" | head -1)
    rm -f "$temp_script"
    
    echo -e "${YELLOW}Cartas disponíveis: $output${NC}"
}

# Registrar usuários
echo -e "\n${BLUE}═══ Passo 1: Registro de Usuários ═══${NC}"
register_user "$SERVER1_URL" "$USER_A" "$PASS" || exit 1
register_user "$SERVER2_URL" "$USER_B" "$PASS" || exit 1

# Fazer login
echo -e "\n${BLUE}═══ Passo 2: Login ═══${NC}"
TOKEN_A=$(login_user "$SERVER1_URL" "$USER_A" "$PASS") || exit 1
TOKEN_B=$(login_user "$SERVER2_URL" "$USER_B" "$PASS") || exit 1

# Criar personagens
echo -e "\n${BLUE}═══ Passo 3: Criação de Personagens ═══${NC}"
create_character "$WS1_URL" "$TOKEN_A" "$USER_A"
create_character "$WS2_URL" "$TOKEN_B" "$USER_B"

sleep 3

# Verificar cartas iniciais
echo -e "\n${BLUE}═══ Passo 4: Verificação de Cartas Iniciais ═══${NC}"
check_cards "$USER_A" "$TOKEN_A" "$SERVER1_URL"
check_cards "$USER_B" "$TOKEN_B" "$SERVER2_URL"

# Propor troca
echo -e "\n${BLUE}═══ Passo 5: Propondo Troca Cross-Server ═══${NC}"
echo -e "${YELLOW}User A (Server 1) propõe troca com User B (Server 2)${NC}"

# Criar logs para capturar mensagens
LOG_A="/tmp/trade_log_${USER_A}.log"
LOG_B="/tmp/trade_log_${USER_B}.log"
rm -f "$LOG_A" "$LOG_B"

# Iniciar listener para User B em background
echo -e "${YELLOW}Iniciando listener para User B no Server 2...${NC}"
websocat "${WS2_URL}?token=${TOKEN_B}" > "$LOG_B" 2>&1 &
PID_B=$!
sleep 2

# Enviar proposta de troca de User A para User B
# Formato: GAME:playerId:TRADE:PROPOSE:targetPlayerId:offeredCard1,offeredCard2:requestedCard1,requestedCard2
echo -e "${YELLOW}User A enviando proposta de troca...${NC}"
TRADE_CMD="GAME:${USER_A}:TRADE:PROPOSE:${USER_B}:basic-0:basic-1"
echo "$TRADE_CMD" | websocat -n "${WS1_URL}?token=${TOKEN_A}" > "$LOG_A" 2>&1 &
PID_A=$!

# Aguardar processamento
echo -e "${YELLOW}Aguardando processamento da proposta...${NC}"
sleep 5

# Verificar se User B recebeu a proposta
echo -e "\n${BLUE}═══ Passo 6: Verificando Recebimento da Proposta ═══${NC}"
if grep -q "TRADE_PROPOSAL" "$LOG_B"; then
    echo -e "${GREEN}✓ User B recebeu a proposta de troca!${NC}"
    
    # Extrair TRADE_ID
    TRADE_ID=$(grep "TRADE_PROPOSAL" "$LOG_B" | head -1 | grep -oP 'UPDATE:TRADE_PROPOSAL:\K[^:]+' | head -1)
    echo -e "${GREEN}Trade ID: $TRADE_ID${NC}"
    
    if [ -n "$TRADE_ID" ]; then
        # User B aceita a troca
        echo -e "\n${BLUE}═══ Passo 7: Aceitando Troca ═══${NC}"
        echo -e "${YELLOW}User B aceitando troca $TRADE_ID...${NC}"
        
        # Parar o listener anterior
        kill $PID_B 2>/dev/null || true
        sleep 1
        
        # Reiniciar listeners para ambos
        websocat "${WS1_URL}?token=${TOKEN_A}" > "$LOG_A" 2>&1 &
        PID_A=$!
        websocat "${WS2_URL}?token=${TOKEN_B}" > "$LOG_B" 2>&1 &
        PID_B=$!
        sleep 2
        
        # Enviar aceitação
        ACCEPT_CMD="GAME:${USER_B}:TRADE:ACCEPT:${TRADE_ID}"
        echo "$ACCEPT_CMD" | websocat -n "${WS2_URL}?token=${TOKEN_B}" 2>&1 | tee -a "$LOG_B" &
        
        # Aguardar execução
        echo -e "${YELLOW}Aguardando execução da troca...${NC}"
        sleep 5
        
        # Verificar resultado
        echo -e "\n${BLUE}═══ Passo 8: Verificando Resultado ═══${NC}"
        
        echo -e "${YELLOW}Log User A:${NC}"
        cat "$LOG_A" | tail -20
        echo ""
        echo -e "${YELLOW}Log User B:${NC}"
        cat "$LOG_B" | tail -20
        echo ""
        
        if grep -q "TRADE_COMPLETE:SUCCESS" "$LOG_A" && grep -q "TRADE_COMPLETE:SUCCESS" "$LOG_B"; then
            echo -e "${GREEN}════════════════════════════════════════${NC}"
            echo -e "${GREEN}✓✓✓ TROCA CROSS-SERVER BEM-SUCEDIDA! ✓✓✓${NC}"
            echo -e "${GREEN}════════════════════════════════════════${NC}"
        else
            echo -e "${RED}════════════════════════════════════════${NC}"
            echo -e "${RED}✗ TROCA FALHOU ✗${NC}"
            echo -e "${RED}════════════════════════════════════════${NC}"
        fi
    else
        echo -e "${RED}✗ Não foi possível extrair TRADE_ID${NC}"
    fi
else
    echo -e "${RED}✗ User B NÃO recebeu a proposta de troca${NC}"
    echo -e "${YELLOW}Conteúdo do log de User B:${NC}"
    cat "$LOG_B"
fi

# Cleanup
kill $PID_A $PID_B 2>/dev/null || true

echo -e "\n${BLUE}═══ Logs salvos em: ═══${NC}"
echo "  User A: $LOG_A"
echo "  User B: $LOG_B"
echo ""
echo -e "${YELLOW}Para verificar logs do servidor:${NC}"
echo "  tail -f dueling-server/logs/application.log | grep TRADE"
echo ""
