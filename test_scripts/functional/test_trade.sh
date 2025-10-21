#!/bin/bash

# Script de teste para funcionalidade de troca de cartas (Trading)
# Este script testa os cenários: aceitação, rejeição e falha de trocas

# Definir variáveis
API_URL="${API_URL:-http://localhost:8080}"
WS_URL="${WS_URL:-ws://localhost:8080/ws}"
USER_A="trader_a_$(date +%s)"
USER_B="trader_b_$(date +%s)"
PASS_A="pa123"
PASS_B="pb123"
CARD_A="basic-0"
CARD_B="basic-1"

# Função para registrar usuário
register_user() {
    local username=$1
    local password=$2
    local playerId=$3
    
    echo "Registrando usuário: $username"
    curl -s -X POST "${API_URL}/api/auth/register" \
        -H "Content-Type: application/json" \
        -d "{\"username\": \"$username\", \"password\": \"$password\", \"playerId\": \"$playerId\"}"
    echo ""
}

# Função para login do usuário
login_user() {
    local username=$1
    local password=$2
    
    echo "Fazendo login do usuário: $username"
    local response=$(curl -s -X POST "${API_URL}/api/auth/login" \
        -H "Content-Type: application/json" \
        -d "{\"username\": \"$username\", \"password\": \"$password\"}")
    
    # Extrai o token da resposta
    local token=$(echo "$response" | jq -r '.token')
    if [ "$token" != "null" ]; then
        echo "$token"
    else
        echo "Erro ao fazer login: $response"
        exit 1
    fi
}

# Função para envio de mensagem via WebSocket usando wscat
send_ws_message() {
    local token=$1
    local message=$2
    
    # Usar websocat ou wscat dependendo do que estiver instalado
    if command -v websocat > /dev/null; then
        echo "$message" | timeout 10s websocat "${WS_URL}?token=${token}" 2>/dev/null
    else
        echo "$message" | timeout 10s wscat -c "${WS_URL}?token=${token}" 2>/dev/null
    fi
}

# Função para esperar que o servidor responda
wait_for_server() {
    local seconds=${1:-2}
    sleep $seconds
}

# Função para verificar se uma mensagem está presente em um arquivo
check_message_in_file() {
    local file=$1
    local message=$2
    local timeout=10
    local count=0
    
    while [ $count -lt $timeout ]; do
        if [ -f "$file" ] && grep -q "$message" "$file"; then
            return 0
        fi
        sleep 1
        ((count++))
    done
    return 1
}

# Preparação
echo "=== Preparação do teste de troca de cartas ==="
echo "API_URL: $API_URL"
echo "WS_URL: $WS_URL"
echo "Usuários: $USER_A e $USER_B"

# Registrar usuários
register_user "$USER_A" "$PASS_A" "$USER_A"
register_user "$USER_B" "$PASS_B" "$USER_B"

# Fazer login
TOKEN_A=$(login_user "$USER_A" "$PASS_A")
TOKEN_B=$(login_user "$USER_B" "$PASS_B")

echo "Tokens obtidos com sucesso"
echo "Token A: ${TOKEN_A:0:10}..."
echo "Token B: ${TOKEN_B:0:10}..."

# Verificar se os tokens não estão vazios
if [ -z "$TOKEN_A" ] || [ -z "$TOKEN_B" ]; then
    echo "Erro: Não foi possível obter tokens válidos"
    exit 1
fi

echo "Execução dos cenários de teste..."

# Testar cenários de troca
# Cenário 1: Troca bem-sucedida com aceite
echo ""
echo "=== Cenário 1: Troca bem-sucedida com aceite ==="

# Criar arquivos de log temporários
PLAYER_A_LOG="playerA_output.log"
PLAYER_B_LOG="playerB_output.log"
rm -f "$PLAYER_A_LOG" "$PLAYER_B_LOG"

# Iniciar listeners (background) para ambos os jogadores
echo "Iniciando listeners WebSocket para ambos os jogadores..."
{
    send_ws_message "$TOKEN_B" "PING" > "$PLAYER_B_LOG" 2>&1 &
    B_PID=$!
    send_ws_message "$TOKEN_A" "PING" > "$PLAYER_A_LOG" 2>&1 &
    A_PID=$!
} || {
    echo "Erro ao iniciar listeners WebSocket"
    exit 1
}

# Aguardar estabelecimento das conexões
wait_for_server 3

# Jogador A propõe uma troca para Jogador B
TRADE_MESSAGE="TRADE:PROPOSE:${USER_B}:${CARD_A}:${CARD_B}"
echo "Jogador A enviando proposta de troca: $TRADE_MESSAGE"
send_ws_message "$TOKEN_A" "$TRADE_MESSAGE"

# Aguardar e verificar se a proposta chegou ao Jogador B
wait_for_server 2

# Verificar se a proposta foi recebida
if check_message_in_file "$PLAYER_B_LOG" "UPDATE:TRADE_PROPOSAL"; then
    echo "✓ Proposta de troca recebida pelo Jogador B"
    
    # Extrair o TRADE_ID da mensagem
    TRADE_ID=$(grep "UPDATE:TRADE_PROPOSAL" "$PLAYER_B_LOG" | tail -n1 | cut -d':' -f4)
    if [ -n "$TRADE_ID" ]; then
        echo "✓ TRADE_ID extraído: $TRADE_ID"
        
        # Jogador B aceita a troca
        ACCEPT_MESSAGE="TRADE:ACCEPT:${TRADE_ID}"
        echo "Jogador B aceitando troca: $ACCEPT_MESSAGE"
        send_ws_message "$TOKEN_B" "$ACCEPT_MESSAGE"
        
        # Aguardar conclusão da troca
        wait_for_server 3
        
        # Verificar se ambos receberam confirmação de sucesso
        if check_message_in_file "$PLAYER_A_LOG" "UPDATE:TRADE_COMPLETE:SUCCESS" && 
           check_message_in_file "$PLAYER_B_LOG" "UPDATE:TRADE_COMPLETE:SUCCESS"; then
            echo "✓ Cenário 1: SUCESSO - Troca completada com sucesso"
        else
            echo "✗ Cenário 1: FALHA - Troca não completada com sucesso"
            echo "Conteúdo do log do Jogador A: $(cat $PLAYER_A_LOG 2>/dev/null || echo 'Não encontrado')"
            echo "Conteúdo do log do Jogador B: $(cat $PLAYER_B_LOG 2>/dev/null || echo 'Não encontrado')"
        fi
    else
        echo "✗ Cenário 1: FALHA - Não foi possível extrair TRADE_ID"
    fi
else
    echo "✗ Cenário 1: FALHA - Jogador B não recebeu proposta de troca"
    echo "Conteúdo do log do Jogador B: $(cat $PLAYER_B_LOG 2>/dev/null || echo 'Não encontrado')"
fi

# Limpar processos de fundo
kill $A_PID $B_PID 2>/dev/null
rm -f "$PLAYER_A_LOG" "$PLAYER_B_LOG"

# Cenário 2: Troca rejeitada
echo ""
echo "=== Cenário 2: Troca rejeitada ==="

# Criar arquivos de log temporários
PLAYER_A_LOG="playerA_output.log"
PLAYER_B_LOG="playerB_output.log"
rm -f "$PLAYER_A_LOG" "$PLAYER_B_LOG"

# Iniciar listeners (background) para ambos os jogadores
echo "Iniciando listeners WebSocket para ambos os jogadores..."
{
    send_ws_message "$TOKEN_B" "PING" > "$PLAYER_B_LOG" 2>&1 &
    B_PID=$!
    send_ws_message "$TOKEN_A" "PING" > "$PLAYER_A_LOG" 2>&1 &
    A_PID=$!
} || {
    echo "Erro ao iniciar listeners WebSocket"
    exit 1
}

# Aguardar estabelecimento das conexões
wait_for_server 3

# Jogador A propõe uma nova troca para Jogador B
TRADE_MESSAGE="TRADE:PROPOSE:${USER_B}:${CARD_A}:${CARD_B}"
echo "Jogador A enviando nova proposta de troca: $TRADE_MESSAGE"
send_ws_message "$TOKEN_A" "$TRADE_MESSAGE"

# Aguardar e verificar se a proposta chegou ao Jogador B
wait_for_server 2

# Verificar se a proposta foi recebida
if check_message_in_file "$PLAYER_B_LOG" "UPDATE:TRADE_PROPOSAL"; then
    echo "✓ Nova proposta de troca recebida pelo Jogador B"
    
    # Extrair o novo TRADE_ID da mensagem
    NEW_TRADE_ID=$(grep "UPDATE:TRADE_PROPOSAL" "$PLAYER_B_LOG" | tail -n1 | cut -d':' -f4)
    if [ -n "$NEW_TRADE_ID" ]; then
        echo "✓ Novo TRADE_ID extraído: $NEW_TRADE_ID"
        
        # Jogador B rejeita a troca
        REJECT_MESSAGE="TRADE:REJECT:${NEW_TRADE_ID}"
        echo "Jogador B rejeitando troca: $REJECT_MESSAGE"
        send_ws_message "$TOKEN_B" "$REJECT_MESSAGE"
        
        # Aguardar resposta à rejeição
        wait_for_server 3
        
        # Verificar se ambos receberam confirmação de rejeição
        if check_message_in_file "$PLAYER_A_LOG" "UPDATE:TRADE_REJECTED_BY_TARGET" && 
           check_message_in_file "$PLAYER_B_LOG" "UPDATE:TRADE_REJECTED"; then
            echo "✓ Cenário 2: SUCESSO - Troca rejeitada corretamente"
        else
            echo "✗ Cenário 2: FALHA - Troca não foi rejeitada corretamente"
            echo "Conteúdo do log do Jogador A: $(cat $PLAYER_A_LOG 2>/dev/null || echo 'Não encontrado')"
            echo "Conteúdo do log do Jogador B: $(cat $PLAYER_B_LOG 2>/dev/null || echo 'Não encontrado')"
        fi
    else
        echo "✗ Cenário 2: FALHA - Não foi possível extrair novo TRADE_ID"
    fi
else
    echo "✗ Cenário 2: FALHA - Jogador B não recebeu nova proposta de troca"
    echo "Conteúdo do log do Jogador B: $(cat $PLAYER_B_LOG 2>/dev/null || echo 'Não encontrado')"
fi

# Limpar processos de fundo
kill $A_PID $B_PID 2>/dev/null
rm -f "$PLAYER_A_LOG" "$PLAYER_B_LOG"

# Cenário 3: Falha quando proponente não tem a carta
echo ""
echo "=== Cenário 3: Falha quando proponente não tem a carta ==="

# Jogador A tenta propor uma troca com uma carta que não possui
FAKE_CARD="card-fake-999"
TRADE_MESSAGE="TRADE:PROPOSE:${USER_B}:${FAKE_CARD}:${CARD_B}"
echo "Jogador A tentando enviar proposta com carta inexistente: $TRADE_MESSAGE"

# Criar log temporário para capturar resposta
PLAYER_A_FAIL_LOG="playerA_fail.log"
rm -f "$PLAYER_A_FAIL_LOG"

# Enviar a tentativa de troca com carta inexistente
response=$(send_ws_message "$TOKEN_A" "$TRADE_MESSAGE" 2>&1)
echo "$response" > "$PLAYER_A_FAIL_LOG"

# Aguardar resposta
wait_for_server 2

# Verificar se a mensagem de erro está presente
if grep -q "ERROR.*does not have all the offered cards" "$PLAYER_A_FAIL_LOG"; then
    echo "✓ Cenário 3: SUCESSO - Jogador corretamente impedido de trocar carta que não possui"
elif grep -q "ERROR" "$PLAYER_A_FAIL_LOG"; then
    # Verifica se é o erro específico que esperamos
    error_content=$(grep "ERROR" "$PLAYER_A_FAIL_LOG")
    echo "Mensagem de erro recebida: $error_content"
    if [[ "$error_content" == *"does not have all the offered cards"* ]]; then
        echo "✓ Cenário 3: SUCESSO - Jogador corretamente impedido de trocar carta que não possui"
    else
        echo "✗ Cenário 3: FALHA - Erro inesperado ao tentar troca com carta inexistente"
        echo "Conteúdo do log: $(cat $PLAYER_A_FAIL_LOG 2>/dev/null || echo 'Não encontrado')"
    fi
else
    echo "✗ Cenário 3: FALHA - Nenhuma mensagem de erro recebida ao tentar troca com carta inexistente"
    echo "Conteúdo do log: $(cat $PLAYER_A_FAIL_LOG 2>/dev/null || echo 'Não encontrado')"
fi

# Remover arquivo de log temporário
rm -f "$PLAYER_A_FAIL_LOG"

# Limpeza final
echo ""
echo "=== Limpeza final ==="
# Garantir que todos os processos em background estejam terminados
pkill -f "wscat\|websocat" 2>/dev/null || true
rm -f player*.log 2>/dev/null || true

echo ""
echo "Teste de troca de cartas concluído!"