# Guia de Como Jogar - Dueling Protocol

Este guia ensina como jogar o Dueling Protocol, tanto em um único PC quanto em múltiplos PCs na mesma rede, usando principalmente o menu interativo `menu.sh`.

## 📋 Índice

- [Visão Geral](#visão-geral)
- [Pré-requisitos](#pré-requisitos)
- [Modo 1: Jogo em Um Único PC](#modo-1-jogo-em-um-único-pc)
- [Modo 2: Jogo em Múltiplos PCs (Mesma Rede)](#modo-2-jogo-em-múltiplos-pcs-mesma-rede)
- [Como Jogar](#como-jogar)
- [Funcionalidades do Jogo](#funcionalidades-do-jogo)
- [Solução de Problemas](#solução-de-problemas)

## Visão Geral

O Dueling Protocol é um jogo de cartas multiplayer distribuído onde:
- Jogadores criam personagens (raça e classe)
- Compram cartas na loja com moedas
- Podem trocar cartas com outros jogadores
- Entram em matchmaking para encontrar oponentes
- Duelam em partidas de cartas

O jogo suporta dois modos:
1. **Single PC**: Todos os componentes rodando em uma máquina
2. **Multi-PC**: Servidores distribuídos em várias máquinas na mesma rede

## Pré-requisitos

### Para Todos os Modos

- **Java 21** ou superior instalado
- **Maven 3.8+** instalado
- **Docker** e **Docker Compose** instalados (recomendado)

### Verificar Instalações

```bash
java -version    # Deve mostrar Java 21+
mvn -version     # Deve mostrar Maven 3.8+
docker --version # Deve mostrar Docker 20.10+
```

## Modo 1: Jogo em Um Único PC

Este modo é ideal para desenvolvimento, testes locais ou jogo solo com bots.

### Passo 1: Preparação

1. Abra um terminal no diretório do projeto
2. Inicie o menu:
   ```bash
   ./menu.sh
   ```

### Passo 2: Compilar o Projeto

```
Escolha a opção: 34 (Build Project)
```

**Aguarde**: 2-5 minutos para compilação completa.

### Passo 3: Escolher Modo de Execução Local

Você tem 3 opções para rodar localmente:

#### Opção A: Sistema Completo com Docker (Recomendado)

```
Escolha a opção: 1 (Start Complete System - Docker + NGINX)
```

**Inclui**:
- PostgreSQL (banco de dados)
- Redis Master e Slaves
- Redis Sentinel (alta disponibilidade)
- NGINX Gateway (load balancer)
- 2 Game Servers (server-1 e server-2)

**Vantagens**:
- Ambiente completo e isolado
- Simula ambiente de produção
- Suporta múltiplos jogadores
- Matchmaking distribuído funcionando

**Aguarde**: 2-3 minutos para todos os serviços iniciarem.

#### Opção B: Jogo Local com Processos Java

```
Escolha a opção: 2 (Start Game Local - Java processes)
```

**Inclui**:
- Infraestrutura em Docker (PostgreSQL, Redis, Sentinel)
- Servidores rodando como processos Java (não containerizados)

**Vantagens**:
- Mais rápido para desenvolver
- Logs mais acessíveis
- Fácil debug

**Aguarde**: 1-2 minutos.

#### Opção C: Jogo Local Simples (Sem Sentinel)

```
Escolha a opção: 3 (Start Game Local Simple - No Sentinel)
```

**Inclui**:
- PostgreSQL e Redis básicos
- 1 Game Server

**Vantagens**:
- Mais leve e rápido
- Ideal para desenvolvimento
- Menos recursos consumidos

**Desvantagens**:
- Sem alta disponibilidade
- Sem failover automático

**Aguarde**: 30-60 segundos.

### Passo 4: Verificar se os Serviços Estão Rodando

```
Escolha a opção: 40 (System Status Check)
```

Você deve ver:
- ✅ Docker is running
- ✅ Active containers (postgres, redis, server-1, etc.)
- ✅ Server processes running

### Passo 5: Executar Cliente(s)

Abra **um novo terminal** (mantenha o menu rodando no primeiro) e execute:

```bash
./menu.sh
```

Depois escolha:
```
Opção: 10 (Run Client)
```

Isso abrirá a interface gráfica do cliente.

**Para múltiplos jogadores locais**: Abra vários terminais e execute a opção 10 em cada um.

### Passo 6: Jogar!

Siga as instruções em [Como Jogar](#como-jogar) abaixo.

### Passo 7: Parar o Jogo

Quando terminar, volte ao menu principal e:

```
Opção: 33 (Stop All Services)
```

Isso para todos os serviços de forma limpa.

## Modo 2: Jogo em Múltiplos PCs (Mesma Rede)

Este modo permite que diferentes jogadores em computadores diferentes na mesma rede joguem juntos, com servidores distribuídos.

### Arquitetura Multi-PC

```
┌─────────────┐         ┌─────────────┐         ┌─────────────┐
│   PC 1      │         │   PC 2      │         │   PC 3      │
│  (Server)   │         │  (Client)   │         │  (Client)   │
│             │         │             │         │             │
│  Gateway    │◄────────┤  Cliente 1  │         │  Cliente 2  │
│  Server 1   │         │             │         │             │
│  Server 2   │         │             │         │             │
│  PostgreSQL │         │             │         │             │
│  Redis      │         │             │         │             │
└─────────────┘         └─────────────┘         └─────────────┘
   192.168.1.100         192.168.1.101          192.168.1.102
```

### Configuração do PC Servidor (PC 1)

Este PC hospedará toda a infraestrutura.

#### Passo 1: Descobrir IP da Máquina

```bash
# Linux/Mac
ip addr show | grep "inet "
# ou
ifconfig | grep "inet "

# Windows (PowerShell)
ipconfig
```

Anote o IP da rede local (exemplo: `192.168.1.100`).

#### Passo 2: Compilar o Projeto

```bash
./menu.sh
```

```
Opção: 34 (Build Project)
```

#### Passo 3: Iniciar Sistema Completo

```
Opção: 1 (Start Complete System)
```

**Aguarde**: 2-3 minutos.

#### Passo 4: Verificar Conectividade

Certifique-se de que o firewall permite conexões na porta 80:

```bash
# Linux (UFW)
sudo ufw allow 80/tcp

# Linux (firewalld)
sudo firewall-cmd --permanent --add-port=80/tcp
sudo firewall-cmd --reload

# Mac
# Vai para Preferências do Sistema > Segurança > Firewall
```

#### Passo 5: Testar Acesso Externo

De outro PC na rede, teste:

```bash
curl http://192.168.1.100/api/health
```

Deve retornar informações de saúde do servidor.

### Configuração dos PCs Clientes (PC 2, PC 3, etc.)

Os PCs clientes precisam apenas do cliente compilado.

#### Passo 1: Copiar Cliente Compilado

Há duas formas:

**Forma A: Clonar o repositório e compilar**

```bash
git clone <repositorio>
cd dueling-protocol
mvn clean package -DskipTests
```

**Forma B: Copiar apenas o JAR do cliente**

No PC servidor:
```bash
# Compactar o cliente
cd dueling-client/target
zip -r dueling-client.zip dueling-client-1.0-SNAPSHOT.jar lib/
```

Transfira `dueling-client.zip` para o PC cliente via USB, rede compartilhada, etc.

No PC cliente:
```bash
unzip dueling-client.zip
```

#### Passo 2: Configurar Endereço do Servidor

Crie ou edite o arquivo de configuração do cliente:

```bash
# No diretório onde está o JAR do cliente
nano application.properties
```

Adicione:
```properties
server.url=ws://192.168.1.100/ws
server.http.url=http://192.168.1.100
```

Substitua `192.168.1.100` pelo IP do PC servidor.

#### Passo 3: Executar Cliente

```bash
java -jar dueling-client-1.0-SNAPSHOT.jar
```

A interface gráfica abrirá conectada ao servidor remoto.

### Configuração Avançada: Múltiplos Servidores em Múltiplos PCs

Para um ambiente ainda mais distribuído, você pode executar servidores em diferentes PCs.

#### PC 1 (Infraestrutura + Server 1)

```bash
./menu.sh
```

```
Opção: 6 (Start Gateway Remote)
```

Isso inicia PostgreSQL, Redis e Gateway.

Em outro terminal:
```
Opção: 7 (Start Server Remote)
```

Configure `SERVER_NAME=server-1` e `SERVER_PORT=8080`.

#### PC 2 (Server 2)

Instale Docker e compile o projeto.

```bash
./menu.sh
```

```
Opção: 7 (Start Server Remote)
```

Configure:
```bash
export SERVER_NAME=server-2
export SERVER_PORT=8083
export POSTGRES_HOST=192.168.1.100
export REDIS_HOST=192.168.1.100
export PEER_SERVERS=http://192.168.1.100:8080
```

#### PC 3+ (Clientes)

Execute os clientes como descrito anteriormente.

## Como Jogar

### Interface do Cliente

Quando o cliente inicia, você verá a tela principal.

### 1. Criar Personagem

1. **Nome**: Digite o nome do seu personagem
2. **Raça**: Escolha entre:
   - Human (Humano)
   - Elf (Elfo)
   - Dwarf (Anão)
   - Orc (Orc)
3. **Classe**: Escolha entre:
   - Warrior (Guerreiro)
   - Mage (Mago)
   - Rogue (Ladino)
   - Cleric (Clérigo)
4. Clique em **"Criar Personagem"** ou envie: `GAME:{playerId}:CHARACTER_SETUP:{nome}:{raça}:{classe}`

### 2. Comprar Cartas

Você começa com **100 moedas**.

1. Vá para a aba **"Loja"** ou **"Shop"**
2. Veja as cartas disponíveis:
   - **Fireball** - 10 moedas - Carta de ataque de fogo
   - **Ice Shard** - 10 moedas - Carta de ataque de gelo
   - **Lightning Bolt** - 15 moedas - Carta de ataque de raio
   - **Healing Potion** - 8 moedas - Carta de cura
   - **Shield** - 12 moedas - Carta de defesa
3. Clique no botão **"Comprar"** da carta desejada
   - Ou envie: `GAME:{playerId}:BUY_CARD:{nomeCard}`

**Importante**: O estoque é limitado! Cartas populares podem esgotar.

### 3. Trocar Cartas (Opcional)

Você pode trocar cartas com outros jogadores online.

#### Propor uma Troca

1. Vá para a aba **"Trocas"** ou **"Trade"**
2. Selecione um jogador online da lista
3. Escolha qual carta você quer dar
4. Escolha qual carta você quer receber
5. Clique em **"Propor Troca"**
   - Ou envie: `GAME:{playerId}:TRADE_PROPOSE:{targetPlayerId}:{suaCarta}:{cartaDesejada}`

#### Aceitar/Rejeitar uma Troca

Quando receber uma proposta:
1. Uma notificação aparecerá
2. Revise os detalhes da troca
3. Clique em **"Aceitar"** ou **"Rejeitar"**
   - Ou envie: `GAME:{playerId}:TRADE_ACCEPT:{tradeId}`
   - Ou envie: `GAME:{playerId}:TRADE_REJECT:{tradeId}`

**Nota**: Trocas entre jogadores em servidores diferentes usam protocolo Two-Phase Commit (2PC) para garantir atomicidade - ou ambos recebem as cartas ou nenhum recebe.

### 4. Entrar no Matchmaking

Quando estiver pronto para duelar:

1. Vá para a aba **"Matchmaking"**
2. Clique em **"Entrar na Fila"**
   - Ou envie: `GAME:{playerId}:MATCHMAKING:ENTER`
3. Aguarde um oponente ser encontrado
4. Você pode estar em servidores diferentes!

O sistema irá:
- Colocá-lo em uma fila global
- Procurar oponentes em todos os servidores
- Criar uma partida quando encontrar alguém

### 5. Durante a Partida

Uma vez que a partida começa:

1. **Sua Vez**: Selecione uma carta e jogue
   - Envie: `GAME:{playerId}:PLAY_CARD:{cardName}`
2. **Turno do Oponente**: Aguarde o oponente jogar
3. **Vencedor**: O jogador com mais pontos ao final vence!

**Comandos durante partida**:
- `GAME:{playerId}:PLAY_CARD:{cardName}` - Jogar carta
- `GAME:{playerId}:SURRENDER` - Desistir (se implementado)

## Funcionalidades do Jogo

### Sistema de Moedas

- **Inicial**: 100 moedas ao criar personagem
- **Gasto**: Comprar cartas na loja
- **Ganho**: Vencer partidas (se implementado)

### Sistema de Estoque

- Cartas têm **quantidade limitada** no estoque global
- Quando o estoque acaba, a carta não pode mais ser comprada
- Reset do estoque: Use `Opção 37 (Reset Card Stock)` no menu

### Sistema de Troca (2PC)

**Troca Local** (mesma instância de servidor):
- Rápida e direta
- Processada localmente

**Troca Cross-Server** (servidores diferentes):
1. **Fase 1 - PREPARE**:
   - Ambos os servidores validam
   - Recursos são bloqueados
   - Se algum falhar, a troca é cancelada
2. **Fase 2 - COMMIT**:
   - Ambos os servidores executam a troca
   - Transação atômica
   - Garante que ambos recebem ou nenhum recebe

### Sistema de Matchmaking Distribuído

- **Fila Global**: Todos os jogadores de todos os servidores em uma fila
- **Cooldown**: Previne tentativas duplicadas de match
- **Coordenação**: Servidores se comunicam via REST API
- **Redis Pub/Sub**: Notificações em tempo real

### Monitoramento (Admin)

Para administradores, no menu principal:

```
Opção: 36 (Check WebSocket Status)    # Verifica conexões WebSocket ativas
Opção: 12 (Monitor All Logs)          # Monitora logs em tempo real
Opção: 38 (View Logs)                 # Visualiza logs salvos
Opção: 35 (View Running Containers)   # Lista containers Docker
```

## Solução de Problemas

### Cliente Não Conecta ao Servidor

**Sintoma**: "Connection refused" ou timeout

**Soluções**:

1. Verifique se o servidor está rodando:
   ```bash
   ./menu.sh
   Opção: 40 (System Status Check)
   ```

2. Verifique se o IP está correto:
   ```bash
   ping 192.168.1.100
   ```

3. Verifique firewall:
   ```bash
   # Linux
   sudo ufw status
   
   # Permitir porta 80
   sudo ufw allow 80/tcp
   ```

4. Teste com curl:
   ```bash
   curl http://192.168.1.100/api/health
   ```

### "Insufficient coins" ao Comprar Carta

**Problema**: Você não tem moedas suficientes.

**Solução**: 
- Cada jogador começa com 100 moedas
- Compre cartas mais baratas primeiro
- Se for teste, reinicie o servidor para resetar moedas

### "Card not available" ao Comprar

**Problema**: Estoque da carta esgotou.

**Solução**: 
```bash
./menu.sh
Opção: 37 (Reset Card Stock)
```

Isso reabastece o estoque de todas as cartas.

### Matchmaking Não Encontra Oponente

**Problema**: Fila vazia ou apenas 1 jogador.

**Soluções**:

1. **Adicione mais jogadores**: Abra mais clientes
2. **Verifique servidores ativos**:
   ```
   Opção: 35 (View Running Containers)
   ```
   Deve haver pelo menos 1 servidor rodando
3. **Espere**: O sistema tenta parear a cada 5 segundos

### Troca Falha com "Trade failed"

**Problemas possíveis**:

1. **Carta não existe**: Verifique os nomes das cartas
2. **Jogador offline**: O outro jogador desconectou
3. **Servidor destino offline**: Em trocas cross-server

**Solução**: Tente novamente ou escolha outro jogador.

### Performance Lenta

**Se o jogo estiver lento:**

1. **Feche containers não usados**:
   ```
   Opção: 33 (Stop All Services)
   ```

2. **Use modo simples** em vez de completo:
   ```
   Opção: 3 (Start Game Local Simple)
   ```

3. **Aumente recursos do Docker**:
   - Docker Desktop > Settings > Resources
   - Aumente CPU e RAM

### Logs para Debug

Para ver logs detalhados:

```bash
# Logs de servidores
docker compose -f docker/docker-compose.yml logs server-1
docker compose -f docker/docker-compose.yml logs server-2

# Logs de PostgreSQL
docker compose -f docker/docker-compose.yml logs postgres

# Logs de Redis
docker compose -f docker/docker-compose.yml logs redis-master

# Todos os logs
./menu.sh
Opção: 12 (Monitor All Logs)
```

## Dicas e Truques

### 🎮 Dicas de Gameplay

- **Compre cartas variadas**: Tenha um deck balanceado
- **Troque cartas duplicadas**: Use o sistema de trocas
- **Entre no matchmaking rápido**: Quanto antes entrar, mais rápido acha oponente
- **Gerencie suas moedas**: Não gaste tudo de uma vez

### 🔧 Dicas Técnicas

- **Múltiplos clientes locais**: Abra vários terminais com `Opção 10`
- **Teste cross-server**: Use `Opção 1` que já inicia 2 servidores
- **Reset rápido**: `Opção 33` (Stop) + `Opção 1` (Start)
- **Debug**: Use `Opção 4 (Start Game Local Debug)` para logs detalhados

### 📊 Comandos Úteis do Menu

| Opção | Comando | Quando Usar |
|-------|---------|-------------|
| 1 | Start Complete System | Iniciar jogo completo |
| 2 | Start Game Local | Desenvolvimento |
| 3 | Start Game Local Simple | Teste rápido |
| 10 | Run Client | Abrir cliente |
| 33 | Stop All Services | Limpar ambiente |
| 34 | Build Project | Após mudanças de código |
| 37 | Reset Card Stock | Recarregar estoque |
| 40 | System Status Check | Verificar o que está rodando |

## Modos de Jogo Sugeridos

### Modo Solo (Com Bots)

```bash
# Iniciar sistema com bot
export BOT_MODE=autobot
export BOT_SCENARIO=""
./menu.sh
Opção: 1
```

### Modo Local Multiplayer (1 PC)

```bash
# Terminal 1: Servidor
./menu.sh
Opção: 1

# Terminal 2: Cliente 1
./menu.sh
Opção: 10

# Terminal 3: Cliente 2
./menu.sh
Opção: 10
```

### Modo LAN Party (Múltiplos PCs)

```bash
# PC Host: Servidor
./menu.sh
Opção: 1

# PC Amigo 1: Cliente
java -jar dueling-client-1.0-SNAPSHOT.jar --server.url=ws://IP_DO_HOST/ws

# PC Amigo 2: Cliente
java -jar dueling-client-1.0-SNAPSHOT.jar --server.url=ws://IP_DO_HOST/ws
```

---

**Divirta-se jogando Dueling Protocol!** 🎮⚔️🃏

Para mais informações técnicas sobre a arquitetura e implementação, consulte o [README.md](../../README.md) principal.
