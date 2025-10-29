# Guia de Execução de Testes - Dueling Protocol

Este guia fornece instruções para executar todos os testes disponíveis no projeto Dueling Protocol usando o menu interativo `menu.sh`.

## 📋 Índice

- [Pré-requisitos](#pré-requisitos)
- [Iniciando o Menu](#iniciando-o-menu)
- [Preparação do Ambiente](#preparação-do-ambiente)
- [Testes Disponíveis](#testes-disponíveis)
- [Executando Todos os Testes](#executando-todos-os-testes)
- [Testes Individuais](#testes-individuais)
- [Interpretando Resultados](#interpretando-resultados)
- [Solução de Problemas](#solução-de-problemas)

## Pré-requisitos

Antes de executar os testes, certifique-se de ter:

- **Docker** e **Docker Compose** instalados e rodando
- **Java 21** ou superior
- **Maven 3.8+** instalado
- Portas necessárias livres (8080, 8083, 5432, 6379, etc.)

## Iniciando o Menu

No diretório raiz do projeto, execute:

```bash
./menu.sh
```

Você verá o menu principal com todas as opções disponíveis.

## Preparação do Ambiente

### 1. Compilar o Projeto (Opção 34)

Antes de executar qualquer teste, compile o projeto:

```
Escolha a opção: 34
```

Isso irá:
- Limpar builds anteriores
- Compilar todos os módulos (servidor, cliente, gateway)
- Criar os arquivos JAR necessários
- Construir as imagens Docker

**Aguarde**: Este processo pode levar de 2-5 minutos.

### 2. Verificar Status do Sistema (Opção 40)

Antes de iniciar os testes, verifique o status:

```
Escolha a opção: 40
```

Isso mostrará:
- Status do Docker
- Containers ativos
- Processos Java rodando

## Testes Disponíveis

### Categorias de Testes

O projeto organiza os testes em diferentes categorias:

1. **Testes Funcionais**: Validam funcionalidades básicas do jogo
2. **Testes de Infraestrutura**: Validam Redis, PostgreSQL, Sentinel
3. **Testes Distribuídos**: Validam comunicação entre servidores
4. **Testes de Concorrência**: Validam operações simultâneas
5. **Testes de Segurança**: Validam JWT e proteções
6. **Testes de Integração**: Validam integração completa

## Executando Todos os Testes

### Opção 9: Run All Tests

Para executar a suíte completa de testes:

```
Escolha a opção: 9
```

**O que este comando faz:**

1. Para todos os serviços rodando
2. Constrói as imagens Docker (se necessário)
3. Executa testes de cenários individuais:
   - Desconexão na fila
   - Desconexão durante partida
   - Jogadas simultâneas
   - Condições de corrida na persistência
   - Inputs malformados
4. Executa testes de integração (Pub/Sub, REST)
5. Executa testes de novos componentes:
   - Gateway
   - JWT Security
   - Redis
   - PostgreSQL
   - Sistema distribuído
   - Integração completa
6. Executa teste de stress (10 clientes)
7. Executa testes finais de validação

**Tempo estimado**: 15-30 minutos

**Logs**: Os resultados são salvos em `test_logs/`

## Testes Individuais

### Testes Funcionais

#### Opção 13: Test Client WebSocket
```
Descrição: Testa conexão WebSocket básica do cliente
Menu: 13
Valida: Conexão, envio/recebimento de mensagens
Tempo: ~30 segundos
```

#### Opção 14: Test Dueling Protocol
```
Descrição: Testa o protocolo de duelo completo
Menu: 14
Valida: Fluxo de jogo, turnos, cartas
Tempo: ~1 minuto
```

#### Opção 17: Test Game State Consistency
```
Descrição: Testa consistência do estado do jogo
Menu: 17
Valida: Sincronização de estado entre servidor e cliente
Tempo: ~1 minuto
```

#### Opção 18: Test Mid-Game Disconnection
```
Descrição: Testa desconexão durante partida
Menu: 18
Valida: Recuperação de desconexão, estado mantido
Tempo: ~1 minuto
```

#### Opção 19: Test Persistence Race Condition
```
Descrição: Testa condições de corrida na persistência
Menu: 19
Valida: Integridade dos dados sob condições de corrida
Tempo: ~1 minuto
```

#### Opção 20: Test Queue Disconnection
```
Descrição: Testa desconexão na fila de matchmaking
Menu: 20
Valida: Remoção da fila, recuperação limpa
Tempo: ~30 segundos
```

#### Opção 21: Test Simultaneous Play
```
Descrição: Testa jogadas simultâneas
Menu: 21
Valida: Sincronização, prevenção de conflitos
Tempo: ~1 minuto
```

#### Opção 26: Test Purchase
```
Descrição: Testa compra de cartas
Menu: 26
Valida: Sistema de loja, transações, estoque
Tempo: ~1 minuto
```

#### Opção 29: Test Matchmaking
```
Descrição: Testa sistema de matchmaking
Menu: 29
Valida: Fila, pareamento, criação de partida
Tempo: ~1 minuto
```

#### Opção 30: Test Trade Functionality
```
Descrição: Testa funcionalidade de troca
Menu: 30
Valida: Propostas, aceitação, rejeição de trocas
Tempo: ~1 minuto
```

#### Opção 31: Test Cross-Server Trade
```
Descrição: Testa troca entre servidores (2PC)
Menu: 31
Valida: Two-Phase Commit, atomicidade, rollback
Tempo: ~2 minutos
Requer: 2+ servidores ativos
```

#### Opção 32: Test Cross-Server Match
```
Descrição: Testa partida entre servidores
Menu: 32
Valida: Matchmaking distribuído, coordenação
Tempo: ~2 minutos
Requer: 2+ servidores ativos
```

### Testes de Infraestrutura

#### Opção 15: Test Redis Sentinel
```
Descrição: Testa Redis Sentinel
Menu: 15
Valida: Failover automático, alta disponibilidade
Tempo: ~2 minutos
```

#### Opção 27: Test Redis Failover
```
Descrição: Testa failover completo do Redis
Menu: 27
Valida: Recuperação automática, eleição de líder
Tempo: ~3 minutos
```

### Testes Distribuídos

#### Opção 16: Test S2S Communication
```
Descrição: Testa comunicação Server-to-Server
Menu: 16
Valida: REST API entre servidores, coordenação
Tempo: ~1 minuto
Requer: 2+ servidores ativos
```

#### Opção 23: Test Cross Server Matchmaking
```
Descrição: Testa matchmaking entre servidores
Menu: 23
Valida: Fila global, cooldown, coordenação
Tempo: ~2 minutos
Requer: 2+ servidores ativos
```

#### Opção 24: Test Global Coordination
```
Descrição: Testa coordenação global
Menu: 24
Valida: Eleição de líder, tasks distribuídas
Tempo: ~2 minutos
Requer: 2+ servidores ativos
```

#### Opção 25: Test Distributed Matchmaking
```
Descrição: Testa matchmaking distribuído completo
Menu: 25
Valida: Sistema completo de matchmaking distribuído
Tempo: ~3 minutos
Requer: 2+ servidores ativos
```

### Testes de Concorrência

#### Opção 22: Test Stock Concurrency
```
Descrição: Testa concorrência no estoque de cartas
Menu: 22
Valida: Locks distribuídos, prevenção de overselling
Tempo: ~2 minutos
```

### Testes de Segurança

#### Opção 28: Test Advanced Security
```
Descrição: Testa segurança avançada
Menu: 28
Valida: JWT, autenticação, autorização
Tempo: ~2 minutos
```

## Interpretando Resultados

### Sinais de Sucesso

Ao executar um teste, procure por:

```
✓ Test PASSED
✓ All checks passed
✓ SUCCESS
```

Mensagens em **verde** indicam sucesso.

### Sinais de Falha

Mensagens em **vermelho** indicam problemas:

```
✗ Test FAILED
✗ ERROR
✗ Connection refused
```

### Logs Detalhados

Os logs detalhados ficam em:
```
test_logs/
├── test_results_<timestamp>.log
├── server_<timestamp>.log
└── client_<timestamp>.log
```

Para visualizar logs durante a execução:

```
Escolha a opção: 38 (View Logs)
```

## Solução de Problemas

### Teste Falha com "Port already in use"

**Solução**: Pare todos os serviços antes de executar testes

```
Escolha a opção: 33 (Stop All Services)
```

Aguarde alguns segundos e tente novamente.

### Teste Falha com "Connection refused"

**Problema**: Serviços não estão rodando

**Solução**: Inicie o sistema completo primeiro

```
Escolha a opção: 1 (Start Complete System)
```

Aguarde 2-3 minutos para os serviços iniciarem.

### Teste Falha com "Database connection error"

**Solução**: Reinicie o PostgreSQL

```bash
cd docker
docker compose restart postgres
```

### Teste Falha com "Redis connection error"

**Solução**: Reinicie o Redis

```bash
cd docker
docker compose restart redis-master redis-sentinel-1 redis-sentinel-2 redis-sentinel-3
```

### Testes Distribuídos Falham

**Problema**: Apenas 1 servidor está rodando

**Solução**: Verifique que múltiplos servidores estão ativos

```
Escolha a opção: 35 (View Running Containers)
```

Você deve ver pelo menos:
- `server-1`
- `server-2`

Se não, inicie o sistema completo:

```
Escolha a opção: 1 (Start Complete System)
```

### Limpar e Reconstruir

Se os testes continuam falando:

1. Pare tudo:
   ```
   Opção: 33 (Stop All Services)
   ```

2. Reconstrua o projeto:
   ```
   Opção: 34 (Build Project)
   ```

3. Inicie o sistema novamente:
   ```
   Opção: 1 (Start Complete System)
   ```

4. Execute os testes:
   ```
   Opção: 9 (Run All Tests)
   ```

## Sequência Recomendada para Testes Completos

Para validar todo o sistema, siga esta sequência:

1. **Compilar** (Opção 34)
2. **Verificar Status** (Opção 40)
3. **Iniciar Sistema Completo** (Opção 1)
4. **Aguardar 2-3 minutos**
5. **Executar Todos os Testes** (Opção 9)
6. **Visualizar Logs** (Opção 38) - se necessário
7. **Parar Serviços** (Opção 33)

## Monitoramento Durante os Testes

Para monitorar os logs em tempo real durante a execução dos testes:

**Em outro terminal**, execute:

```
Escolha a opção: 12 (Monitor All Logs)
```

Isso mostrará logs de todos os serviços em tempo real.

## Dicas Importantes

### ✅ Boas Práticas

- Sempre pare os serviços antes de iniciar novos testes
- Aguarde alguns segundos entre parar e iniciar serviços
- Verifique o status do sistema antes de executar testes
- Salve logs importantes antes de parar os serviços

### ⚠️ Cuidados

- Testes distribuídos requerem múltiplos servidores
- Testes de stress podem consumir muitos recursos
- Alguns testes modificam dados no banco (use banco de testes)
- Testes de failover podem levar alguns minutos

### 📊 Métricas de Sucesso

Um sistema saudável deve ter:
- ✅ Todos os testes funcionais passando
- ✅ Testes de infraestrutura com failover bem-sucedido
- ✅ Testes distribuídos com coordenação funcionando
- ✅ Testes de concorrência sem deadlocks
- ✅ Testes de segurança sem vulnerabilidades

---

**Nota**: Este guia foi criado para facilitar a execução dos testes usando o menu interativo. Para mais detalhes sobre a arquitetura e implementação, consulte o README.md principal.
