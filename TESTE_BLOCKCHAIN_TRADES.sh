#!/bin/bash

# GUIA COMPLETO DE TESTE - CORREÇÃO DO BLOCKCHAIN TRADES

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║     🧪 TESTE COMPLETO: REGISTRO DE TROCAS NO BLOCKCHAIN       ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

cat << 'EOF'
📋 PASSO A PASSO PARA TESTAR A CORREÇÃO:

PASSO 1: VERIFICAR SISTEMA ESTÁ RODANDO
========================================
Execute:
  docker ps | grep -E "dueling-blockchain|server-"

Esperado: 
  ✅ dueling-blockchain rodando
  ✅ server-1, server-2, server-3, server-4 rodando

Se não estiver rodando:
  cd docker && docker compose up -d


PASSO 2: REGISTRAR DOIS JOGADORES E COMPRAR PACOTES
====================================================
Abra 2 terminais diferentes e execute em cada um:

Terminal 1:
  ./menu.sh
  Opção 10 (Start Client)
  
Terminal 2:  
  ./menu.sh
  Opção 10 (Start Client)

Em cada cliente:
  1. Registre um jogador (ex: "Jogador1" e "Jogador2")
  2. Compre um pacote de cartas (opção 2 no menu)
  3. Liste suas cartas (opção 4)
  4. Anote os IDs das cartas


PASSO 3: VERIFICAR MINTAGEM NO BLOCKCHAIN
==========================================
Execute:
  docker exec dueling-blockchain npx hardhat run --network localhost scripts/verify-ledger.js

Esperado:
  💳 CARDS MINTED (from pack purchases): 10 cards
  📦 Jogador1 cards (5 total): Token #0, #1, #2, #3, #4
  📦 Jogador2 cards (5 total): Token #5, #6, #7, #8, #9


PASSO 4: EXECUTAR UMA TROCA
============================
No Terminal 1 (Jogador1):
  1. No menu do cliente, escolha "Propor troca"
  2. Digite o ID do Jogador2
  3. Selecione uma carta sua (ex: basic-0)
  4. Selecione uma carta do Jogador2 (ex: basic-1)

No Terminal 2 (Jogador2):
  1. Você receberá uma notificação de proposta de troca
  2. Use a opção "Aceitar troca"
  3. Digite o ID da troca proposta

Ambos os jogadores devem receber:
  ✅ "UPDATE:TRADE_COMPLETE:SUCCESS"


PASSO 5: VERIFICAR TROCA NO BLOCKCHAIN
=======================================
Execute:
  docker exec dueling-blockchain npx hardhat run --network localhost scripts/verify-ledger.js

Esperado:
  💳 CARDS MINTED: 10 cards
  
  🔄 CARD TRANSFERS (trades): 2 transfers
  
  🤝 Trade #1:
     Jogador1 gave:
       → Token #0 - basic-0 | Trap (Common) [ATK:50 DEF:1]
              ⇅ TRADE ⇅
     Jogador2 gave:
       → Token #5 - basic-1 | Trap (Common) [ATK:50 DEF:1]


PASSO 6: VERIFICAR LOGS DO SERVIDOR
====================================
Execute:
  docker logs server-1 | grep "Recording trade"

Esperado:
  🔄 Recording trade on blockchain - Jogador1 ↔ Jogador2
     Player1 (Jogador1) trading 1 cards (tokenIds: [0])
     Player2 (Jogador2) trading 1 cards (tokenIds: [5])
  ✅ Trade xyz recorded on blockchain - 2 cards transferred


📊 COMO INTERPRETAR OS RESULTADOS
==================================

✅ SUCESSO: Se você vê as trocas listadas no blockchain
   - O número de CARD TRANSFERS deve ser > 0
   - Deve aparecer detalhes da troca com os jogadores e cartas
   - Logs mostram "Trade recorded on blockchain"

❌ FALHA: Se não vê as trocas
   - Verifique se o blockchain está rodando
   - Verifique logs: docker logs server-1 | grep -i error
   - Certifique-se de que a troca foi completada (ambos aceitaram)


🔍 COMANDOS ÚTEIS DE DIAGNÓSTICO
=================================

Ver status de todos containers:
  docker ps --format "table {{.Names}}\t{{.Status}}"

Ver logs do blockchain:
  docker logs dueling-blockchain | tail -50

Ver logs de um servidor específico:
  docker logs server-1 | grep -i blockchain

Reiniciar tudo do zero:
  cd docker && docker compose down && docker compose up -d


💡 DICAS
========

1. Aguarde 5-10 segundos após comprar pacotes antes de fazer trocas
   (tempo para o blockchain mintar as cartas)

2. Se uma troca falhar, verifique os logs imediatamente

3. Você pode fazer múltiplas trocas e todas devem aparecer no ledger

4. Use o comando de verificação quantas vezes quiser - ele é somente leitura


📚 DOCUMENTAÇÃO
===============

Para mais detalhes sobre a correção implementada:
  cat FIX_BLOCKCHAIN_TRADES.md

EOF

echo ""
echo "╔════════════════════════════════════════════════════════════════╗"
echo "║          ✅ GUIA DE TESTE CARREGADO                            ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""
echo "Execute os passos acima para testar a correção completa!"
echo ""
