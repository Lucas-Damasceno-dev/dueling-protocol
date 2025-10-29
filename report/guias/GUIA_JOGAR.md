# 🎮 Guia de Como Jogar - Dueling Protocol

**Versão:** 1.0  
**Data:** 2025-11-03  
**Objetivo:** Guia completo para jogar o Dueling Protocol (PC único ou rede local)

---

## 🎯 Modos de Jogo

### 1. 🖥️ Um PC (Single Player Local)
Perfeito para testar e jogar contra você mesmo ou com alguém no mesmo computador.

### 2. 🌐 Múltiplos PCs (Rede Local/LAN)
Para jogar com amigos na mesma rede (casa, escritório, etc).

---

## 🚀 Modo 1: Um PC (Single Player)

### Passo 1: Iniciar o Sistema

```bash
cd /home/lucas/Documentos/dev/projects/dueling-protocol
bash menu.sh
```

**Escolha a opção:** `1` (Start Complete System)

Aguarde até ver:
```
✓ Container server-1 healthy
✓ Container nginx-gateway healthy
System started successfully!
```

---

### Passo 2: Abrir Dois Terminais

Você vai precisar de **2 terminais** (ou abas) para jogar contra você mesmo:

#### Terminal 1 - Jogador 1
```bash
cd /home/lucas/Documentos/dev/projects/dueling-protocol
bash menu.sh
# Escolha opção: 10 (Run Client)
```

#### Terminal 2 - Jogador 2
```bash
cd /home/lucas/Documentos/dev/projects/dueling-protocol
bash menu.sh
# Escolha opção: 10 (Run Client)
```

---

### Passo 3: Registrar e Logar (Ambos Jogadores)

Em **cada terminal**, você verá:

```
=== DUELING PROTOCOL - CLIENT ===
Choose an option:
1. Register
2. Login
3. Exit
>
```

#### No Terminal 1 (Jogador 1):
```
> 1                          # Escolha Register
Username: player1            # Digite seu username
Password: senha123           # Digite sua senha
✓ Registration successful!

> 2                          # Escolha Login
Username: player1
Password: senha123
✓ Login successful!
```

#### No Terminal 2 (Jogador 2):
```
> 1                          # Escolha Register
Username: player2            # Digite OUTRO username
Password: senha456           # Digite outra senha
✓ Registration successful!

> 2                          # Escolha Login
Username: player2
Password: senha456
✓ Login successful!
```

---

### Passo 4: Criar Personagem (Ambos Jogadores)

Após o login, você verá o menu principal:

```
=== MAIN MENU ===
1. Create Character
2. Buy Pack
3. View Collection
4. Manage Decks
5. Trade Cards
6. Enter Matchmaking
7. View Profile
0. Logout
>
```

#### Criar personagem em AMBOS terminais:

```
> 1                          # Create Character
Enter character name: Aragorn
Choose race:
1. HUMAN
2. ELF
3. DWARF
4. ORC
> 1                          # Escolha HUMAN
Choose class:
1. WARRIOR
2. MAGE
3. ROGUE
> 1                          # Escolha WARRIOR
✓ Character created successfully!
```

---

### Passo 5: Comprar Pacotes de Cartas (Ambos Jogadores)

Antes de jogar, você precisa de cartas!

```
> 2                          # Buy Pack
Available packs:
1. BASIC (100 gold)
2. EPIC (500 gold)
Choose pack: 1
✓ Pack purchased!
You received 5 cards:
- Basic Card 0
- Basic Card 1
- Light Sword
- ...
```

**Dica:** Compre pelo menos 1 pacote em cada jogador para ter cartas suficientes.

---

### Passo 6: Entrar no Matchmaking (Ambos Jogadores)

Agora é hora de encontrar uma partida!

#### Terminal 1:
```
> 6                          # Enter Matchmaking
Entering matchmaking queue...
✓ Entered matchmaking queue
Waiting for opponent...
```

#### Terminal 2:
```
> 6                          # Enter Matchmaking
Entering matchmaking queue...
✓ Match found!
Opponent: player1
```

**O match será criado automaticamente em ~2 segundos!**

---

### Passo 7: Jogar!

Quando o match for criado, você verá:

```
=== MATCH STARTED ===
Match ID: abc123
Opponent: player2
Your turn!

Your hand:
1. Basic Card 0 (Attack: 5)
2. Light Sword (Equipment, +3 Attack)
3. Basic Card 1 (Attack: 3)

Actions:
1. Play card
2. Attack
3. End turn
>
```

#### Comandos durante a partida:

**Jogar uma carta:**
```
> 1                          # Play card
Which card? (1-3): 1
✓ Played Basic Card 0
```

**Atacar:**
```
> 2                          # Attack
✓ You attacked opponent for 5 damage!
Opponent HP: 15/20
```

**Finalizar turno:**
```
> 3                          # End turn
✓ Turn ended. Waiting for opponent...
```

---

### Passo 8: Trocar Cartas (Opcional)

Você pode trocar cartas com outros jogadores fora de partidas:

#### Terminal 1 (Proposer):
```
> 5                          # Trade Cards
Your cards:
1. Basic Card 0
2. Light Sword
3. Basic Card 1

Choose card to offer: 2      # Ofereço Light Sword
Target player ID: 2          # ID do player2 (pode ver no perfil dele)
Choose card to request: 1    # Quero Basic Card 0 dele
✓ Trade proposal sent!
```

#### Terminal 2 (Accepter):
```
✓ Trade proposal received!
From: player1
Offering: Light Sword
Requesting: Basic Card 0
Accept? (y/n): y
✓ Trade completed!
```

---

## 🌐 Modo 2: Múltiplos PCs (Rede Local)

### Setup (Apenas no PC Principal)

#### 1. Descobrir o IP Local

No PC que vai hospedar os servidores:

```bash
# Linux/Mac
ip addr show | grep "inet " | grep -v 127.0.0.1

# Windows
ipconfig
```

Você verá algo como: `192.168.1.100`

#### 2. Iniciar os Serviços

```bash
cd /home/lucas/Documentos/dev/projects/dueling-protocol
bash menu.sh
# Opção 1: Start Complete System
```

#### 3. Verificar Firewall

Certifique-se de que a porta 8080 está aberta:

```bash
# Linux (Ubuntu/Debian)
sudo ufw allow 8080

# Verificar
sudo ufw status
```

---

### Nos Outros PCs (Jogadores)

#### 1. Instalar o Cliente

Em cada PC adicional:

```bash
# Clone o repositório OU copie apenas a pasta dueling-client
git clone <repo-url>
cd dueling-protocol/dueling-client

# Ou use um pendrive para copiar a pasta dueling-client
```

#### 2. Configurar o IP do Servidor

Edite o arquivo de configuração ou use variáveis de ambiente:

```bash
# Linux/Mac
export GATEWAY_HOST=192.168.1.100  # IP do PC principal
export GATEWAY_PORT=8080

# Windows
set GATEWAY_HOST=192.168.1.100
set GATEWAY_PORT=8080
```

#### 3. Iniciar o Cliente

```bash
# Se tiver Java instalado:
java -DGATEWAY_HOST=192.168.1.100 -DGATEWAY_PORT=8080 -jar target/dueling-client-1.0-SNAPSHOT.jar

# Ou usando o menu (se copiou o projeto completo):
bash menu.sh
# Opção 10: Run Client
```

#### 4. Registrar e Jogar

Siga os passos 3-8 do **Modo 1** acima. A diferença é que cada jogador está em um PC diferente!

---

## 🎮 Comandos Rápidos Durante o Jogo

### Menu Principal
| Comando | Ação |
|---------|------|
| `1` | Criar Personagem |
| `2` | Comprar Pacote |
| `3` | Ver Coleção |
| `4` | Gerenciar Decks |
| `5` | Trocar Cartas |
| `6` | Entrar no Matchmaking |
| `7` | Ver Perfil |
| `0` | Logout |

### Durante a Partida
| Comando | Ação |
|---------|------|
| `1` | Jogar Carta |
| `2` | Atacar |
| `3` | Finalizar Turno |
| `SURRENDER` | Desistir |

### Comandos Especiais (via WebSocket)
Para usuários avançados que querem usar comandos diretos:

```
CHARACTER_SETUP:Nome:RAÇA:CLASSE
STORE:BUY:BASIC
MATCHMAKING:ENTER
TRADE:PROPOSE:targetPlayerId:offeredCards:requestedCards
TRADE:ACCEPT:tradeId
PLAY_CARD:matchId:cardId
ATTACK:matchId:targetId
END_TURN:matchId
```

---

## 🏆 Dicas e Estratégias

### 1. Gerenciamento de Recursos
- Comece comprando pacotes BASIC (são mais baratos)
- Gerencie bem seu mana para jogar cartas
- Economize cartas fortes para momentos críticos

### 2. Construção de Deck
- Balance entre criaturas e equipamentos
- Tenha pelo menos 30 cartas no deck
- Inclua cartas de diferentes custos de mana

### 3. Durante o Combate
- Ataque quando o oponente tiver menos vida
- Use equipamentos para fortalecer suas criaturas
- Finalize o turno quando não puder mais jogar

### 4. Trocas
- Troque cartas duplicadas
- Negocie com outros jogadores
- Use o chat (se disponível) para negociar

---

## 🐛 Problemas Comuns

### "Connection refused"
**Solução:** Verifique se os serviços estão rodando:
```bash
docker ps
# Deve mostrar: server-1, nginx-gateway, postgres, redis
```

### "Invalid credentials"
**Solução:** Certifique-se de que registrou primeiro (opção 1) antes de logar (opção 2).

### "Match timeout"
**Solução:** Ambos jogadores devem entrar no matchmaking em até ~30 segundos um do outro.

### "Card not found"
**Solução:** Compre pacotes primeiro (opção 2) antes de entrar em uma partida.

### Não consigo conectar de outro PC
**Solução:**
1. Verifique o IP do servidor: `ip addr show`
2. Teste conectividade: `ping 192.168.1.100`
3. Verifique firewall: `sudo ufw allow 8080`
4. Use o IP correto no cliente

---

## 📱 Atalhos do Menu.sh

Para facilitar, use o menu.sh:

```bash
bash menu.sh
```

**Opções úteis:**
- `1`: Iniciar todo o sistema
- `10`: Rodar cliente (jogar)
- `33`: Parar todos os serviços
- `34`: Build (se fez mudanças)
- `40`: Ver status do sistema
- `44`: Testar se tudo está funcionando

---

## 🎓 Tutorial Completo (5 minutos)

### Para Iniciantes:

1. **Abra um terminal**
   ```bash
   cd /home/lucas/Documentos/dev/projects/dueling-protocol
   bash menu.sh
   ```

2. **Inicie o sistema** (opção `1`)
   - Aguarde 30 segundos

3. **Abra OUTRO terminal** e rode o cliente (opção `10`)

4. **Registre-se** (opção `1` no cliente)
   - Username: `jogador1`
   - Password: `senha`

5. **Faça login** (opção `2` no cliente)

6. **Crie personagem** (opção `1` no menu principal)
   - Nome: `Heroi`
   - Raça: `HUMAN`
   - Classe: `WARRIOR`

7. **Compre cartas** (opção `2`)
   - Pack: `BASIC`

8. **Repita passos 3-7 em outro terminal** para criar o jogador 2

9. **Entre no matchmaking** (opção `6`) em AMBOS terminais

10. **Jogue!** 🎉

---

## 🎉 Pronto!

Agora você sabe como jogar o Dueling Protocol tanto em um PC quanto em rede local. 

**Divirta-se!** ⚔️🎮

Para mais informações, consulte:
- `GUIA_TESTES.md` - Como testar o sistema
- `README.md` - Documentação técnica
- `report/` - Relatórios e documentação adicional
