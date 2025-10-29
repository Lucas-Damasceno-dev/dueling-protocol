# 🧪 Guia de Execução de Testes - Dueling Protocol

**Versão:** 1.0  
**Data:** 2025-11-03  
**Objetivo:** Guia completo para executar todos os testes do projeto usando o menu.sh

---

## 🎯 Testes Críticos (Funcionalidades Principais)

### ✅ Teste 1: PURCHASE (Compra de Pacotes)

**O que testa:**
- Sistema de compra de pacotes de cartas
- Adição de cartas ao inventário do jogador
- Comunicação WebSocket para notificação de compra

**Como executar:**

```bash
cd /home/lucas/Documentos/dev/projects/dueling-protocol
bash menu.sh
# Escolha opção: 41
```

**Resultado esperado:**
```
✅ PURCHASE SUCCESS
1. PURCHASE: ✅ PASSED
```

**Fluxo do teste:**
1. Registra um novo usuário
2. Faz login e obtém token JWT
3. Conecta via WebSocket
4. Cria personagem
5. Envia comando `STORE:BUY:BASIC`
6. Verifica se recebeu 5 cartas
7. ✅ Sucesso se cartas foram adicionadas

---

### ✅ Teste 2: TRADE (Troca de Cartas)

**O que testa:**
- Sistema de proposta de troca entre jogadores
- Aceitação de trocas
- Transferência de cartas entre inventários
- Notificações via Pub/Sub

**Como executar:**

```bash
cd /home/lucas/Documentos/dev/projects/dueling-protocol
bash menu.sh
# Escolha opção: 42
```

**Resultado esperado:**
```
✅ TRADE PROPOSAL RECEIVED
✅ TRADE COMPLETE
2. TRADE: ✅ PASSED
```

**Fluxo do teste:**
1. Registra dois usuários (trader1, trader2)
2. Ambos fazem login e criam personagens
3. Conectam via WebSocket
4. trader1 propõe troca: `TRADE:PROPOSE:trader2Id:basic-0:basic-1`
5. trader2 recebe notificação de proposta
6. trader2 aceita: `TRADE:ACCEPT:tradeId`
7. Sistema executa a troca
8. Ambos recebem `UPDATE:TRADE_COMPLETE:SUCCESS`
9. ✅ Sucesso se troca foi completada

---

### ✅ Teste 3: MATCHMAKING (Sistema de Partidas)

**O que testa:**
- Fila de matchmaking
- Criação automática de partidas
- Notificação de match criado
- Sistema cross-server de matchmaking

**Como executar:**

```bash
cd /home/lucas/Documentos/dev/projects/dueling-protocol
bash menu.sh
# Escolha opção: 43
```

**Resultado esperado:**
```
✅ MATCHMAKING SUCCESS
3. MATCHMAKING: ✅ PASSED
```

**Fluxo do teste:**
1. Registra dois usuários (match1, match2)
2. Ambos fazem login e criam personagens
3. Conectam via WebSocket
4. Ambos entram na fila: `MATCHMAKING:ENTER`
5. Sistema automaticamente cria match (a cada 2s)
6. Ambos recebem notificação de match criado
7. ✅ Sucesso se match foi criado

---

### 🎯 Teste 4: TODOS OS TESTES CRÍTICOS

**O que testa:**
- Executa os 3 testes acima em sequência
- Verifica integridade de todas as funcionalidades principais

**Como executar:**

```bash
cd /home/lucas/Documentos/dev/projects/dueling-protocol
bash menu.sh
# Escolha opção: 44
```

**Resultado esperado:**
```
╔════════════════════════════════════════════════╗
║                    RESUMO                      ║
╚════════════════════════════════════════════════╝
1. PURCHASE:    ✅ PASSED
2. TRADE:       ✅ PASSED
3. MATCHMAKING: ✅ PASSED
```

---

## 🔧 Outros Testes Disponíveis

### Teste 5: Build do Projeto

**Como executar:**
```bash
bash menu.sh
# Escolha opção: 34
```

**O que faz:**
- Compila todo o projeto Maven
- Executa testes unitários
- Cria imagens Docker
- Verifica integridade do build

---

### Teste 6: Testes de Matchmaking (Legacy)

**Como executar:**
```bash
bash menu.sh
# Escolha opção: 29
```

**O que testa:**
- Sistema de matchmaking (versão legacy)
- Fila local de jogadores

---

### Teste 7: Teste de Compra (Legacy)

**Como executar:**
```bash
bash menu.sh
# Escolha opção: 26
```

**O que testa:**
- Sistema de compra de pacotes (versão legacy)

---

### Teste 8: Teste de Troca (Legacy)

**Como executar:**
```bash
bash menu.sh
# Escolha opção: 30
```

**O que testa:**
- Sistema de troca de cartas (versão legacy)

---

## 🚀 Fluxo Completo de Testes

### Pré-requisitos:
```bash
# 1. Certifique-se de que Docker está instalado e rodando
docker --version
docker compose version

# 2. Certifique-se de que Node.js está instalado
node --version  # Deve ser v14+

# 3. Navegue até o diretório do projeto
cd /home/lucas/Documentos/dev/projects/dueling-protocol
```

### Passo a Passo:

#### 1️⃣ Build do Projeto
```bash
bash menu.sh  # Opção 34
```
Aguarde até ver: `BUILD SUCCESS`

#### 2️⃣ Inicie os Serviços
```bash
bash menu.sh  # Opção 1 (Start Complete System)
```
Aguarde cerca de 30 segundos para os serviços iniciarem

#### 3️⃣ Execute os Testes Críticos
```bash
bash menu.sh  # Opção 44 (Test ALL Critical Features)
```

#### 4️⃣ Verifique os Resultados
Você deve ver:
```
✅ PURCHASE SUCCESS
✅ TRADE COMPLETE  
✅ MATCHMAKING SUCCESS
```

---

## 🐛 Solução de Problemas

### Problema: "Connection refused"
**Causa:** Serviços Docker não estão rodando  
**Solução:**
```bash
bash menu.sh  # Opção 1
# Aguarde 30 segundos e tente novamente
```

### Problema: "User already exists"
**Causa:** Teste anterior não limpou os usuários  
**Solução:**
```bash
# Os testes usam timestamp único, então isso não deve acontecer
# Mas se acontecer, pare e reinicie os serviços:
bash menu.sh  # Opção 33 (Stop All)
bash menu.sh  # Opção 1 (Start)
```

### Problema: "Test timeout"
**Causa:** Serviços demorando para responder  
**Solução:**
```bash
# Verifique os logs:
docker logs server-1
docker logs nginx-gateway

# Reinicie se necessário:
bash menu.sh  # Opção 33
bash menu.sh  # Opção 1
```

### Problema: "Cannot find module 'ws'"
**Causa:** Dependências Node.js não instaladas  
**Solução:**
```bash
cd test_scripts
npm install ws
```

---

## 📊 Interpretando os Resultados

### ✅ Teste PASSOU
```
✅ PURCHASE SUCCESS
1. PURCHASE: ✅ PASSED
```
Significa que a funcionalidade está operacional e todos os passos foram executados com sucesso.

### ❌ Teste FALHOU
```
❌ PURCHASE TIMEOUT
1. PURCHASE: ❌ FAILED
```
Significa que houve um problema. Verifique:
1. Serviços estão rodando? (`docker ps`)
2. Logs têm erros? (`docker logs server-1`)
3. Portas estão disponíveis? (`netstat -tuln | grep 8080`)

---

## 🎓 Comandos Úteis

### Ver logs de um serviço:
```bash
docker logs server-1        # Server 1
docker logs nginx-gateway   # Gateway
docker logs redis-master    # Redis
```

### Ver serviços rodando:
```bash
bash menu.sh  # Opção 35
```

### Parar todos os serviços:
```bash
bash menu.sh  # Opção 33
```

### Verificar status:
```bash
bash menu.sh  # Opção 40
```

---

## 📝 Notas Importantes

1. **Testes são Destrutivos**: Cada teste cria novos usuários. Não afeta dados de produção.

2. **Testes são Independentes**: Cada teste pode ser executado separadamente.

3. **Timeout Padrão**: Testes têm timeout de 15-25 segundos. Se demorar mais, algo está errado.

4. **Portas Necessárias**:
   - 8080: Servidor principal
   - 5432: PostgreSQL
   - 6379: Redis
   - Certifique-se de que estão livres

5. **Recursos Mínimos**:
   - RAM: 4GB
   - CPU: 2 cores
   - Disco: 2GB livre

---

## 🎉 Conclusão

Este guia cobre todos os testes disponíveis no projeto. Para uso diário:

1. **Desenvolvimento**: Use opções 41-43 para testar funcionalidades específicas
2. **CI/CD**: Use opção 44 para teste completo
3. **Build**: Use opção 34 antes de fazer commit
4. **Deploy**: Use opção 1 para subir ambiente completo

**Dúvidas?** Verifique a documentação em `report/guias/` ou os logs dos serviços.
