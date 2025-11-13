# Dueling Protocol - Blockchain Smart Contracts

## ⚠️ IMPORTANTE: Status de Integração

**O módulo blockchain está FUNCIONAL mas NÃO está integrado ao jogo Java.**

- ✅ **Smart contracts**: Implementados e testados
- ✅ **Blockchain local**: Funcionando (Hardhat Network)  
- ❌ **Integração com jogo**: Não implementada

**O que isso significa:**
- Compras, trocas e partidas no jogo são registradas apenas no PostgreSQL
- Para testar a blockchain, use os scripts de simulação (veja abaixo)
- A integração blockchain é um recurso futuro planejado

## 📋 Visão Geral

Este módulo contém os smart contracts do Dueling Protocol implementados em Solidity para Ethereum. Migração do Problema 2 (coordenação centralizada) para o Problema 3 (blockchain descentralizada).

## 🏗️ Arquitetura

### Smart Contracts Desenvolvidos

1. **AssetContract.sol** (ERC-721)
   - Gerenciamento de cartas como NFTs únicos
   - Propriedade imutável e transferível
   - Metadados on-chain (tipo, raridade, ataque, defesa)

2. **StoreContract.sol**
   - Compra de pacotes de cartas
   - Prevenção de duplo gasto
   - Geração pseudo-aleatória de cartas
   - Suporte a 3 tipos de pacotes (Bronze, Silver, Gold)

3. **TradeContract.sol**
   - Trocas atômicas entre jogadores
   - Proposta, aceite e cancelamento de trocas
   - Validação de propriedade em tempo de execução
   - Transferências reversíveis apenas antes da confirmação

4. **MatchContract.sol**
   - Registro imutável de resultados de partidas
   - Proof-of-play através de game state hash
   - Estatísticas de jogadores (vitórias, derrotas, win rate)
   - Apenas servidor autorizado pode registrar

## 🚀 Tecnologias

- **Solidity**: ^0.8.20
- **Hardhat**: Framework de desenvolvimento
- **OpenZeppelin**: Bibliotecas de contratos seguros
- **Ethers.js**: Integração JavaScript/TypeScript
- **Rede de Desenvolvimento**: Hardhat Network (local)
- **Rede de Produção**: Ethereum Sepolia Testnet

## 📦 Instalação

```bash
npm install
```

## 🔨 Compilação

```bash
npm run compile
```

## 🧪 Testes

```bash
npm test
```

## 🌐 Deploy

### Local (Hardhat Network)
```bash
# Terminal 1: Iniciar nó local
npm run node

# Terminal 2: Deploy dos contratos
npm run deploy:local
```

### Sepolia Testnet
```bash
# Configurar variáveis de ambiente
cp .env.example .env
# Editar .env com PRIVATE_KEY e SEPOLIA_RPC_URL

# Deploy
npm run deploy:sepolia
```

## 📁 Estrutura do Projeto

```
dueling-blockchain/
├── contracts/              # Smart contracts Solidity
│   ├── AssetContract.sol
│   ├── StoreContract.sol
│   ├── TradeContract.sol
│   └── MatchContract.sol
├── scripts/                # Scripts de deploy
│   └── deploy.js
├── test/                   # Testes unitários
│   ├── AssetContract.test.js
│   ├── StoreContract.test.js
│   ├── TradeContract.test.js
│   └── MatchContract.test.js
├── hardhat.config.js       # Configuração Hardhat
└── package.json
```

## 🔐 Segurança

### Implementações de Segurança

- **ReentrancyGuard**: Proteção contra ataques de reentrância (StoreContract)
- **Ownable**: Controle de acesso baseado em proprietário (AssetContract)
- **Access Control**: Apenas gameServer autorizado (MatchContract)
- **Input Validation**: Validações rigorosas em todos os contratos
- **Atomic Operations**: Trocas e compras são atômicas (tudo ou nada)

### Limitações Conhecidas

- **Randomness**: Pseudo-aleatório on-chain (não é criptograficamente seguro)
  - Para produção, usar Chainlink VRF
- **Gas Optimization**: Implementação inicial não otimizada para gas
- **Scalability**: Ethereum L1 tem limitações de throughput

## 📊 Fluxos de Dados

### Compra de Pacote
```
Cliente → MetaMask → StoreContract.purchasePack()
         ↓
    AssetContract.mintCard() × 5
         ↓
    PackPurchased event
         ↓
    Gateway escuta evento
         ↓
    Notifica cliente via WebSocket
```

### Troca de Cartas
```
Jogador A → TradeContract.proposeTrade()
         ↓
    TradeProposed event
         ↓
Jogador B → TradeContract.acceptTrade()
         ↓
    AssetContract.safeTransferFrom() × N
         ↓
    TradeAccepted event
```

### Registro de Partida
```
GameServer → MatchContract.recordMatch()
         ↓
    Armazena Match struct
         ↓
    Atualiza estatísticas
         ↓
    MatchRecorded event
         ↓
    Dados públicos e auditáveis
```

## 🌍 Transparência

Todos os dados cruciais são públicos e auditáveis:

- **Posse de Cartas**: Qualquer um pode verificar quais cartas um jogador possui
- **Histórico de Compras**: Todas as compras de pacotes são públicas
- **Trocas**: Histórico completo de trocas entre jogadores
- **Resultados de Partidas**: Registro imutável de todas as partidas
- **Proof-of-Play**: Hash do estado do jogo para verificação

### Explorers

- **Sepolia Testnet**: https://sepolia.etherscan.io/
- **Local**: Hardhat Console

## 🔗 Testando a Blockchain

### 1. Simular uma Compra

```bash
npm run simulate:purchase
```

Isso criará uma transação de compra na blockchain com 5 cartas NFT.

### 2. Verificar Propriedade das Cartas

```bash
# Usando o endereço padrão
npm run verify:ownership

# Ou especificando um endereço
PLAYER_ADDRESS=0xSeuEndereco npm run verify:ownership
```

### 3. Ver Histórico de Compras

```bash
npm run verify:purchases
```

### 4. Ver Estatísticas de Partidas

```bash
npm run verify:matches
```

## 🔗 Integração com Gateway Java (PLANEJADO - NÃO IMPLEMENTADO)

O gateway Java (dueling-gateway) usa Web3j para interagir com os contratos:

```java
// Exemplo: Comprar pacote
StoreContract store = StoreContract.load(
    contractAddress,
    web3j,
    credentials,
    gasProvider
);

TransactionReceipt receipt = store.purchasePack(
    BigInteger.valueOf(packType)
).send();
```

## 📈 Métricas de Gas

| Operação | Gas Estimado |
|----------|--------------|
| Mint Card | ~100,000 |
| Purchase Pack (5 cards) | ~500,000 |
| Propose Trade | ~150,000 |
| Accept Trade | ~200,000 |
| Record Match | ~80,000 |

*Valores aproximados, variam com complexidade da transação*

## 🔄 Migração do Problema 2

### Componentes Removidos
- ❌ LockService (locks distribuídos)
- ❌ TradeService (Two-Phase Commit)
- ❌ ServerRegistry (coordenação de servidores)
- ❌ LeaderElectionService
- ❌ Redisson (Redis locks)

### Componentes Adicionados
- ✅ AssetContract (propriedade descentralizada)
- ✅ StoreContract (compras sem coordenação)
- ✅ TradeContract (trocas atômicas)
- ✅ MatchContract (resultados imutáveis)

## 🔍 Verificação para Jogadores

### Documentação

- **[HOW_PLAYERS_VERIFY.md](HOW_PLAYERS_VERIFY.md)** - Resumo: 5 formas de verificar cartas
- **[PLAYER_VERIFICATION_GUIDE.md](PLAYER_VERIFICATION_GUIDE.md)** - Guia completo (15 páginas)
- **[QUICK_START_PLAYERS.md](QUICK_START_PLAYERS.md)** - Comandos rápidos

### Scripts de Verificação

```bash
# Ver todas suas cartas
PLAYER_ADDRESS=0xSeuEndereço npm run verify:ownership

# Verificar uma carta específica
TOKEN_ID=1047 npm run verify:card

# Ver histórico de compras
PLAYER_ADDRESS=0xSeuEndereço npm run verify:purchases
```

### Verificação Visual (Sem Código)

- **Etherscan**: https://sepolia.etherscan.io/
- **MetaMask**: Importe NFTs diretamente na carteira

## 📝 TODO

- [x] Implementar testes unitários completos (68/68 passando)
- [x] Script de deploy automatizado
- [x] Scripts de verificação para jogadores
- [x] Documentação completa de verificação
- [ ] Integração com Chainlink VRF (randomness seguro)
- [ ] Otimização de gas
- [ ] Deploy em Sepolia
- [ ] Verificação de contratos no Etherscan
- [ ] Interface UI no cliente para visualizar NFTs

## 🤝 Contribuindo

Este é um projeto acadêmico (LARSID/UFPI). Contribuições devem seguir as diretrizes do curso.

## 📄 Licença

MIT

## 🎓 Créditos

Projeto desenvolvido para a disciplina de Sistemas Distribuídos - LARSID/UFPI
Problema 03: Blockchain e Ledger Distribuído

---

**Prazo de Entrega**: 09/12/2025
**Status**: 🚧 Em Desenvolvimento
