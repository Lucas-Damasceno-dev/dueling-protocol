#!/bin/bash

# Garante que o script pare se algum comando falhar
set -e

echo "======================================================="
echo ">>> INICIANDO SUÍTE DE TESTES COMPLETA"
echo "======================================================="

# Passo 1: Compilar e construir as imagens Docker (só precisa ser feito uma vez)
echo ">>> [ETAPA 1/3] Construindo as imagens Docker..."
./build.sh
docker-compose build
echo ">>> Imagens construídas com sucesso."
echo ""

# -----------------------------------------------------------------------------

# Passo 2: Executando os testes de cenário e robustez
echo ">>> [ETAPA 2/3] Executando testes de cenário individuais..."

# Função auxiliar para rodar um teste específico
run_test() {
  local bot_mode=$1
  local bot_scenario=$2
  local test_name=$3
  local client_count=$4

  echo "-------------------------------------------------------"
  echo ">>> Rodando teste: $test_name"
  echo "-------------------------------------------------------"

  # Exporta as variáveis para que o docker-compose possa usá-las
  export BOT_MODE=$bot_mode
  export BOT_SCENARIO=$bot_scenario

  # Inicia os contêineres e espera um tempo para o teste ser concluído
  docker-compose up --scale client=$client_count --remove-orphans -d
  sleep 15 # Aumente se os testes precisarem de mais tempo
  
  # Exibe os logs do servidor para análise
  echo ">>> Logs do Servidor para o teste '$test_name':"
  docker-compose logs server
  
  # Limpa o ambiente para o próximo teste
  docker-compose down
  echo ">>> Teste '$test_name' concluído."
  echo ""
}

# Cenários de Desconexão Abrupta
run_test "autobot" "matchmaking_disconnect" "Desconexão na Fila" 2
run_test "autobot" "mid_game_disconnect" "Desconexão no Meio da Partida" 2

# Cenários de Concorrência
run_test "autobot" "simultaneous_play" "Jogada Simultânea" 2
run_test "autobot" "race_condition" "Race Condition na Persistência" 1

# Teste de Robustez com Entradas Malformadas
run_test "maliciousbot" "" "Entradas Malformadas (Malicious Bot)" 1

echo ">>> Testes de cenário individuais concluídos."
echo ""

# -----------------------------------------------------------------------------

# Passo 3: Executando o teste de estresse final
echo ">>> [ETAPA 3/3] Executando o teste de estresse com 10 clientes..."
./stress_test.sh

echo "======================================================="
echo ">>> SUÍTE DE TESTES COMPLETA FINALIZADA"
echo "======================================================="