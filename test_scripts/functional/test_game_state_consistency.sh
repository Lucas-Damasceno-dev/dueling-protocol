#!/usr/bin/env bash

# test_scripts/functional/test_game_state_consistency.sh

# Garante que o script pára se qualquer comando falhar
set -e

# Define o caminho para o ficheiro docker-compose
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
PROJECT_ROOT="$SCRIPT_DIR/../.."
DOCKER_COMPOSE_FILE="$PROJECT_ROOT/docker/docker-compose.yml"

# Verificar se wscat está instalado
if ! command -v wscat &> /dev/null; then
    echo ">>> [ERRO] wscat não está instalado. Por favor, instale com: npm install -g wscat"
    exit 1
fi

# Função de limpeza para parar e remover os containers
cleanup() {
  echo ""
  echo ">>> [CLEANUP] Parando e removendo containers..."
  docker compose -f "$DOCKER_COMPOSE_FILE" down -v
  rm -f /tmp/client_a.log /tmp/client_b.log /tmp/server1.log /tmp/server2.log
  echo ">>> [CLEANUP] Ambiente limpo."
}

# Define a função de limpeza para ser executada na saída do script
trap cleanup EXIT

echo "======================================================="
echo ">>> INICIANDO TESTE DE CONSISTÊNCIA E JUSTIÇA DO ESTADO DO JOGO"
echo "======================================================="

# 3. Setup do Ambiente
echo ">>> [SETUP] Parando serviços antigos..."
docker compose -f "$DOCKER_COMPOSE_FILE" down -v

echo ">>> [SETUP] Iniciando ambiente completo..."
docker compose -f "$DOCKER_COMPOSE_FILE" up --build -d server-1 server-2 nginx-gateway postgres redis-master redis-sentinel-1 redis-slave redis-sentinel-2 redis-sentinel-3

echo ">>> [SETUP] Aguardando 40 segundos para a estabilização dos serviços..."
sleep 40

# 4. Fase de Autenticação
echo ">>> [AUTH] Fase de autenticação..."

# Gerar nomes de usuários únicos
PLAYER_A="player_A_$$"
PLAYER_B="player_B_$$"

echo ">>> [AUTH] Registrando jogador A: $PLAYER_A"
TOKEN_A=$(curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$PLAYER_A\",\"password\":\"password123\"}" | \
  curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$PLAYER_A\",\"password\":\"password123\"}" | \
  grep -o '"token":"[^"]*"' | cut -d'"' -f4)

if [ -z "$TOKEN_A" ]; then
  # Tenta fazer login apenas se o registro não funcionou como esperado
  TOKEN_A=$(curl -s -X POST http://localhost:8080/api/auth/login \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"$PLAYER_A\",\"password\":\"password123\"}" | \
    grep -o '"token":"[^"]*"' | cut -d'"' -f4)
fi

echo ">>> [AUTH] Registrando jogador B: $PLAYER_B"
TOKEN_B=$(curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$PLAYER_B\",\"password\":\"password123\"}" | \
  curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$PLAYER_B\",\"password\":\"password123\"}" | \
  grep -o '"token":"[^"]*"' | cut -d'"' -f4)

if [ -z "$TOKEN_B" ]; then
  # Tenta fazer login apenas se o registro não funcionou como esperado
  TOKEN_B=$(curl -s -X POST http://localhost:8080/api/auth/login \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"$PLAYER_B\",\"password\":\"password123\"}" | \
    grep -o '"token":"[^"]*"' | cut -d'"' -f4)
fi

if [ -z "$TOKEN_A" ] || [ -z "$TOKEN_B" ]; then
    echo ">>> [ERRO] Falha ao obter tokens de autenticação para os jogadores"
    exit 1
fi

echo ">>> [AUTH] Jogador A autenticado com token: ${TOKEN_A:0:10}..."
echo ">>> [AUTH] Jogador B autenticado com token: ${TOKEN_B:0:10}..."

# 5. Fase de Jogo (Conexões WebSocket)
echo ">>> [GAME] Iniciando conexões WebSocket..."

# Iniciar conexões WebSocket para ambos os jogadores
echo ">>> [GAME] Iniciando conexão para Jogador A..."
wscat -c "ws://localhost:8080/ws?token=$TOKEN_A" --execute "CHARACTER_SETUP:TesterA:Human:Warrior" > /tmp/client_a.log 2>&1 &

# Aguardar um momento antes de iniciar o segundo cliente
sleep 2

echo ">>> [GAME] Iniciando conexão para Jogador B..."
wscat -c "ws://localhost:8080/ws?token=$TOKEN_B" --execute "CHARACTER_SETUP:TesterB:Elf:Mage" > /tmp/client_b.log 2>&1 &

# Aguardar um momento para a configuração do personagem
sleep 3

# Enviar MATCHMAKING:ENTER para ambos os jogadores
echo ">>> [GAME] Enviando MATCHMAKING:ENTER para Jogador A..."
wscat -c "ws://localhost:8080/ws?token=$TOKEN_A" --execute "MATCHMAKING:ENTER" > /tmp/client_a_match.log 2>&1 &

sleep 1

echo ">>> [GAME] Enviando MATCHMAKING:ENTER para Jogador B..."
wscat -c "ws://localhost:8080/ws?token=$TOKEN_B" --execute "MATCHMAKING:ENTER" > /tmp/client_b_match.log 2>&1 &

# 6. Esperar Início da Partida
echo ">>> [GAME] Aguardando início da partida..."

# Aguardar até 60 segundos para que ambas as mensagens de início de jogo apareçam
SECONDS=0
TIMEOUT=60
while [ $SECONDS -lt $TIMEOUT ]; do
  if grep -q "UPDATE:GAME_START:" /tmp/client_a.log 2>/dev/null && grep -q "UPDATE:GAME_START:" /tmp/client_b.log 2>/dev/null; then
    echo ">>> [GAME] Partida iniciada!"
    # Extrair MATCH_ID (a partir da mensagem GAME_START)
    MATCH_ID_A=$(grep "UPDATE:GAME_START:" /tmp/client_a.log | head -n1 | cut -d':' -f3- | cut -d',' -f1)
    MATCH_ID_B=$(grep "UPDATE:GAME_START:" /tmp/client_b.log | head -n1 | cut -d':' -f3- | cut -d',' -f1)
    echo ">>> [GAME] MATCH_ID A: $MATCH_ID_A"
    echo ">>> [GAME] MATCH_ID B: $MATCH_ID_B"
    break
  fi
  sleep 2
done

if [ $SECONDS -ge $TIMEOUT ]; then
  echo ">>> [ERRO] Timeout aguardando início da partida"
  exit 1
fi

# 7. Identificar o Turno
echo ">>> [GAME] Aguardando identificação do turno..."

# Aguardar até 30 segundos para a mensagem de novo turno aparecer
SECONDS=0
TIMEOUT=30
while [ $SECONDS -lt $TIMEOUT ]; do
  if grep -q "UPDATE:NEW_TURN:" /tmp/client_a.log 2>/dev/null || grep -q "UPDATE:NEW_TURN:" /tmp/client_b.log 2>/dev/null; then
    # Pegar a informação do turno de qualquer um dos logs
    NEW_TURN_LINE=$(grep "UPDATE:NEW_TURN:" /tmp/client_a.log 2>/dev/null || grep "UPDATE:NEW_TURN:" /tmp/client_b.log 2>/dev/null | head -n1)
    echo ">>> [GAME] Mensagem de novo turno detectada: $NEW_TURN_LINE"
    
    # Extrair o jogador atual (current player) e o jogador esperando (waiting player)
    # O formato é UPDATE:NEW_TURN:[CURRENT_PLAYER_ID]:[WAITING_PLAYER_ID]:...
    CURRENT_PLAYER=$(echo "$NEW_TURN_LINE" | cut -d':' -f3)
    WAITING_PLAYER=$(echo "$NEW_TURN_LINE" | cut -d':' -f4)
    echo ">>> [GAME] Current Player: $CURRENT_PLAYER"
    echo ">>> [GAME] Waiting Player: $WAITING_PLAYER"
    break
  fi
  sleep 1
done

if [ $SECONDS -ge $TIMEOUT ]; then
  echo ">>> [ERRO] Timeout aguardando identificação do turno"
  exit 1
fi

# 8. Executar a Condição de Corrida (O Teste)
echo ">>> [GAME] Executando condição de corrida - jogadas simultâneas..."

# Determinar os comandos com base nos IDs dos jogadores
CURRENT_PLAYER_COMMAND="PLAY_CARD:$MATCH_ID_A:basic-0"
WAITING_PLAYER_COMMAND="PLAY_CARD:$MATCH_ID_A:basic-1"

echo ">>> [GAME] Enviando: $CURRENT_PLAYER_COMMAND para o jogador atual"
echo ">>> [GAME] Enviando: $WAITING_PLAYER_COMMAND para o jogador esperando"

# Enviar os comandos de jogar carta simultaneamente
wscat -c "ws://localhost:8080/ws?token=$TOKEN_A" --execute "$CURRENT_PLAYER_COMMAND" > /tmp/client_a_play.log 2>&1 &
wscat -c "ws://localhost:8080/ws?token=$TOKEN_B" --execute "$WAITING_PLAYER_COMMAND" > /tmp/client_b_play.log 2>&1 &

# Aguardar um tempo para os logs se estabilizarem
sleep 10

# 9. Verificação e Assert (A Demonstração)
echo ">>> [VERIFICATION] Verificando resultados..."

# Verificação de Justiça (Cliente)
echo ">>> [VERIFICATION] Verificando justiça (turno do jogador esperando)..."
if grep -q "ERROR:NOT_YOUR_TURN" /tmp/client_b_play.log 2>/dev/null; then
    echo -e "\e[32m>>> [SUCESSO] O jogador esperando (B) foi corretamente impedido de jogar fora do turno (JUSTIÇA GARANTIDA)\e[0m"
    JUSTICE_MAINTAINED=true
else
    echo -e "\e[31m>>> [FALHA - JUSTIÇA VIOLADA] O jogador esperando (B) conseguiu jogar fora do turno\e[0m"
    JUSTICE_MAINTAINED=false
fi

# Verificação de Consistência (Servidor)
echo ">>> [VERIFICATION] Verificando consistência nos logs dos servidores..."
docker compose -f "$DOCKER_COMPOSE_FILE" logs server-1 > /tmp/server1.log
docker compose -f "$DOCKER_COMPOSE_FILE" logs server-2 > /tmp/server2.log

# Verificar se ambos os servidores processaram playCard
SERVER1_PLAYED=$(grep -c "playCard" /tmp/server1.log 2>/dev/null || echo 0)
SERVER2_PLAYED=$(grep -c "playCard" /tmp/server2.log 2>/dev/null || echo 0)

echo ">>> [VERIFICATION] Server-1 processou $SERVER1_PLAYED jogada(s)"
echo ">>> [VERIFICATION] Server-2 processou $SERVER2_PLAYED jogada(s)"

if [ "$SERVER1_PLAYED" -gt 0 ] && [ "$SERVER2_PLAYED" -gt 0 ]; then
    echo -e "\e[31m>>> [FALHA - CONSISTÊNCIA VIOLADA] Ambos os servidores processaram jogadas, indicando condição de corrida\e[0m"
    CONSISTENCY_MAINTAINED=false
else
    echo -e "\e[32m>>> [SUCESSO] Apenas um servidor processou jogadas, mantendo a consistência\e[0m"
    CONSISTENCY_MAINTAINED=true
fi

# 10. Resultado do Teste
echo "======================================================="
if [ "$JUSTICE_MAINTAINED" = true ] && [ "$CONSISTENCY_MAINTAINED" = true ]; then
    echo -e "\e[32m>>> [RESULTADO] TESTE APROVADO - Justiça e Consistência mantidas\e[0m"
else
    echo -e "\e[31m>>> [RESULTADO] TESTE FALHOU - Justiça ou Consistência violadas\e[0m"
    echo -e "\e[31m>>> [RESULTADO] Justiça mantida: $JUSTICE_MAINTAINED\e[0m"
    echo -e "\e[31m>>> [RESULTADO] Consistência mantida: $CONSISTENCY_MAINTAINED\e[0m"
fi
echo "======================================================="

echo ">>> [INFO] Consulte os logs em /tmp/ para mais detalhes:"
echo ">>> [INFO] - /tmp/client_a.log, /tmp/client_b.log - Logs dos clientes"
echo ">>> [INFO] - /tmp/server1.log, /tmp/server2.log - Logs dos servidores"