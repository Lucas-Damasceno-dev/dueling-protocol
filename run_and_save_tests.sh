#!/bin/bash

# Define o nome do arquivo de log com a data e hora atual para mantê-los organizados
LOG_FILE="test_results_$(date +'%Y-%m-%d_%H-%M-%S').log"

echo "======================================================="
echo ">>> EXECUTANDO TODOS OS TESTES E SALVANDO A SAÍDA"
echo ">>> A saída será exibida aqui e também salva no arquivo: $LOG_FILE"
echo "======================================================="

# Executa o script de testes e redireciona a saída
# O comando 'tee' permite que a saída seja exibida no terminal E salva no arquivo ao mesmo tempo.
# '2>&1' garante que tanto a saída padrão quanto os erros sejam capturados.
./run_all_tests.sh 2>&1 | tee "$LOG_FILE"

echo "======================================================="
echo ">>> EXECUÇÃO CONCLUÍDA"
echo ">>> Os resultados completos foram salvos em: $LOG_FILE"
echo "======================================================="