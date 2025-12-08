# Dueling Protocol

A distributed multiplayer card game system built with microservices architecture, featuring cross-server matchmaking, atomic card trading using distributed locks, real-time WebSocket communication, and **Oracle Pattern blockchain integration** for tamper-evident auditability.

## 📑 Table of Contents

- [Overview](#overview)
- [Distributed Architecture](#distributed-architecture)
  - [Component Diagram](#component-diagram)
  - [Sequence Diagram](#sequence-diagram)
- [Oracle Pattern Integration](#oracle-pattern-integration)
- [Key Features](#key-features)
- [Technologies](#technologies)
- [System Requirements](#system-requirements)
- [Installation](#installation)
- [Running the Project](#running-the-project)
  - [Full Distributed Setup](#full-distributed-setup)
  - [Local Development](#local-development)
  - [Production Mode](#production-mode)
- [Communication Protocol](#communication-protocol)
- [Blockchain Verification](#blockchain-verification)
- [Testing](#testing)
- [Project Structure](#project-structure)
- [Configuration](#configuration)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)
- [License](#license)

## Overview

**Dueling Protocol** is a production-ready distributed game server system that demonstrates modern microservices patterns and distributed systems concepts. The system supports multiple game servers running simultaneously, with players on different servers able to interact seamlessly through cross-server matchmaking, atomic card trading, and real-time match coordination.

### Core Capabilities

- 🎮 **Cross-Server Matchmaking**: Players on different server instances can be matched together
- 🔄 **Atomic Card Trading**: Distributed locks with Redis ensure consistency across servers
- 💳 **Card Shop System**: Buy cards with atomic stock management and transaction safety
- ⚡ **Real-time Communication**: WebSocket-based bidirectional communication with Redis Pub/Sub
- 🔐 **Distributed Locking**: Redisson-based distributed locks prevent race conditions
- 📊 **High Availability**: Redis Sentinel for automatic failover
- 🎯 **Horizontal Scalability**: Stateless server instances with shared persistence layer
- 🔗 **Oracle Pattern Blockchain**: Tamper-evident proofs for all critical operations

## Oracle Pattern Integration

**NEW**: This system implements the **Oracle Pattern** for blockchain integration, where:

- **PostgreSQL** maintains operational data for performance
- **Backend Java servers** act as trusted oracles
- **Blockchain (Ethereum)** stores cryptographic proofs (SHA-256 hashes) of all operations
- **Anyone** can verify data integrity by comparing database records with blockchain proofs

This approach provides:
- ✅ **Tamper-evident storage** - Any data modification is immediately detectable
- ✅ **Public auditability** - Open API for integrity verification
- ✅ **Performance** - Fast operations with PostgreSQL
- ✅ **Decentralized truth** - Blockchain is the ultimate authority

See [ORACLE_PATTERN_IMPLEMENTATION.md](ORACLE_PATTERN_IMPLEMENTATION.md) for detailed documentation.

## 🔗 Blockchain Integration Status

### Current Status: ✅ FULLY INTEGRATED AND OPERATIONAL

The blockchain module is **production-ready** with complete integration:

#### ✅ What's Working:
- **Smart Contracts**: Deployed and tested (AssetContract, TradeContract, MatchContract, StoreContract, IntegrityContract, OracleRegistry)
- **Backend Integration**: Java backend writes all critical operations to blockchain
- **Event Listeners**: Real-time synchronization via Web3j event listeners
- **Transparency API**: REST endpoints for public blockchain verification
- **Cross-Server Sync**: Blockchain events propagated via Redis Pub/Sub

#### 📊 Data Storage Model:
- **PostgreSQL**: Primary operational database (fast queries, complex joins)
- **Blockchain**: Immutable audit trail and proof-of-ownership
- **Dual-Write Strategy**: Critical operations written to both systems

#### 🎯 What's Recorded On-Chain:
- ✅ Card minting (NFT creation with metadata)
- ✅ Card trades (atomic transfers between players)
- ✅ Match results (immutable win/loss records)
- ✅ Player statistics (cumulative stats)
- ✅ Purchase history (provenance tracking)

#### 🔍 Transparency & Auditability:
- **API Endpoints**: `/api/blockchain/*` for public queries
- **Etherscan Integration**: Auto-generated explorer links
- **Verification Methods**: 4 ways to verify data (API, Etherscan, Scripts, Web3 direct)
- **User Guides**: [VERIFICACAO_USUARIO.md](VERIFICACAO_USUARIO.md) (Portuguese), [BLOCKCHAIN_TRANSPARENCY_GUIDE.md](BLOCKCHAIN_TRANSPARENCY_GUIDE.md) (English)

#### 🌐 Network Support:
- **Local Development**: Hardhat Network (localhost:8545)
- **Testnet**: Sepolia (deployment scripts ready)
- **Mainnet**: Configuration ready (not deployed yet)

#### 📖 Documentation:
- Main blockchain README: [dueling-blockchain/README.md](dueling-blockchain/README.md)
- Transparency guide: [BLOCKCHAIN_TRANSPARENCY_GUIDE.md](BLOCKCHAIN_TRANSPARENCY_GUIDE.md)
- User verification guide: [VERIFICACAO_USUARIO.md](VERIFICACAO_USUARIO.md)
- Oracle pattern: [ORACLE_PATTERN_IMPLEMENTATION.md](ORACLE_PATTERN_IMPLEMENTATION.md)

### Quick Start - Blockchain

```bash
# Start local blockchain
cd dueling-blockchain
npm install
npm run node  # Terminal 1

# Deploy contracts
npm run deploy:local  # Terminal 2

# Verify blockchain integration
curl http://localhost:8080/api/blockchain/info

# Check player cards on-chain
curl http://localhost:8080/api/blockchain/cards/0xYOUR_ADDRESS
```

### Deploy to Sepolia Testnet

```bash
cd dueling-blockchain
cp .env.example .env
# Edit .env with your Infura/Alchemy key and private key
npm run deploy:sepolia
npm run verify:contracts  # Verify on Etherscan
```

**Get testnet ETH**: https://sepoliafaucet.com

## ⚠️ Known Limitations

### Current Limitations:

1. **Blockchain Network**
   - ❗ **Not deployed to mainnet** - Currently runs on localhost/Sepolia testnet only
   - ❗ **Gas costs** - Mainnet deployment would require gas optimization
   - ⚠️ **Single signer** - Oracle uses single private key (should be multi-sig in production)

2. **Scalability Constraints**
   - ❗ **Shared Database** - PostgreSQL is a single point of bottleneck
   - ⚠️ **Redis Single Instance** - Can be upgraded to Redis Cluster for higher throughput
   - ⚠️ **Stateless Servers** - Good for horizontal scaling but session data in Redis

3. **Blockchain Sync**
   - ⚠️ **Eventual Consistency** - Blockchain confirmations take 1-2 blocks (~15-30 seconds)
   - ⚠️ **Reorg Risk** - Very rare but possible on testnets (wait 12 blocks for finality)

4. **Security Considerations**
   - ❗ **Private Key Management** - Oracle private key stored in env var (use KMS in production)
   - ❗ **No Rate Limiting** - REST API should have rate limits in production
   - ⚠️ **CORS Enabled** - Currently allows all origins (`origins = "*"`)

5. **Performance**
   - ⚠️ **Blockchain Writes Async** - Cards appear in database before blockchain confirmation
   - ⚠️ **No Connection Pooling for Blockchain** - Single Web3j instance per server
   - ⚠️ **Sequential Blockchain Writes** - Nonce management requires serialization

6. **Monitoring & Observability**
   - ❗ **No Metrics Collection** - Should integrate Prometheus/Grafana
   - ❗ **No Distributed Tracing** - Should add OpenTelemetry for cross-service tracing
   - ⚠️ **Basic Health Checks** - Should add comprehensive readiness/liveness probes

7. **Testing**
   - ⚠️ **Limited Integration Tests** - Blockchain tests mostly unit-level
   - ⚠️ **No Load Testing** - Performance under high load not validated
   - ⚠️ **No Chaos Engineering** - Resilience testing needed

### Planned Improvements:

#### High Priority:
- [ ] Deploy to Ethereum mainnet
- [ ] Implement multi-sig oracle pattern
- [ ] Add rate limiting and DDoS protection
- [ ] Upgrade to Redis Cluster
- [ ] Implement comprehensive monitoring (Prometheus + Grafana)

#### Medium Priority:
- [ ] Add distributed tracing (OpenTelemetry)
- [ ] Optimize gas usage in smart contracts
- [ ] Implement connection pooling for blockchain RPC
- [ ] Add comprehensive integration tests
- [ ] Implement automatic fallback for RPC providers

#### Low Priority:
- [ ] Support multiple blockchain networks simultaneously
- [ ] Add GraphQL API for complex queries
- [ ] Implement IPFS for card artwork storage
- [ ] Add WebSocket subscriptions for blockchain events

### Workarounds:

**For Production Deployment:**

1. **Blockchain Gas Costs**: Use L2 solution (Polygon, Arbitrum, Optimism) for cheaper transactions
2. **Database Bottleneck**: Implement read replicas with master-slave replication
3. **Redis Single Point of Failure**: Use Redis Sentinel (already configured) or Redis Cluster
4. **Private Key Security**: Migrate to AWS KMS, HashiCorp Vault, or hardware security module
5. **Rate Limiting**: Add NGINX rate limiting or use cloud WAF (Cloudflare, AWS WAF)

## Distributed Architecture

The system follows a **microservices architecture** with **shared-database** pattern for consistency and **event-driven communication** for real-time coordination.

### Architecture Overview

```
┌─────────────┐
│   Clients   │ (JavaFX GUI)
└──────┬──────┘
       │ WebSocket / HTTP
       ▼
┌─────────────┐
│    NGINX    │ (Load Balancer / Reverse Proxy)
│   Gateway   │
└──────┬──────┘
       │
       ├──────────┬──────────┬──────────┐
       ▼          ▼          ▼          ▼
   ┌──────┐  ┌──────┐  ┌──────┐    ┌──────┐
   │Server│  │Server│  │Server│... │Server│
   │  1   │  │  2   │  │  3   │    │  N   │
   │:8080 │  │:8083 │  │:808X │    │:808X │
   └───┬──┘  └───┬──┘  └───┬──┘    └───┬──┘
       │         │         │            │
       └─────────┴─────────┴────────────┘
                 │         │
         ┌───────┴────┬────┴────────┐
         ▼            ▼             ▼
    ┌──────────┐ ┌─────────┐  ┌────────────┐
    │PostgreSQL│ │  Redis  │  │   Redis    │
    │ Database │ │ Master  │  │  Sentinel  │
    │  :5432   │ │  :6379  │  │  Cluster   │
    └──────────┘ └─────────┘  └────────────┘
```

### Component Diagram

The following diagram shows the physical structure and relationships between system components:

![Component Diagram](report/problem02/components.svg)

**Key Components:**

1. **Client Layer**: JavaFX-based GUI communicating via WebSocket
2. **Gateway Layer**: NGINX reverse proxy for load balancing and routing
3. **Server Cluster**: Multiple stateless server instances running game logic
4. **Infrastructure Services**:
   - **PostgreSQL**: Single source of truth for persistent data (players, matches, cards)
   - **Redis Sentinel Cluster**: High-availability cache, Pub/Sub broker, and leader election
   - **Sentinels**: Automatic failover and monitoring

### Sequence Diagram

The following diagram illustrates the complete flow of key operations:

![Sequence Diagram](report/problem02/sequence.svg)

**Flows Demonstrated:**

1. **Phase 1-2**: Player connection and character setup
2. **Phase 3**: Card purchase with atomic transaction
3. **Phase 4**: Cross-server card trade using distributed locks
4. **Phase 5-6**: Cross-server matchmaking with HTTP coordination
5. **Phase 7-8**: Match creation and game initialization with Redis Pub/Sub

## Key Features

### ✅ Distributed Systems Features

- **Cross-Server Matchmaking**: 
  - Players queue on different servers
  - Servers coordinate via REST API to find partners
  - Cooldown mechanism prevents race conditions
  - Scheduled tasks ensure matches are created

- **Atomic Card Trading**:
  - Distributed locks via Redisson ensure exclusive access
  - Trade proposals stored in Redis for cross-server visibility
  - Synchronized card collection updates prevent race conditions
  - Transaction rollback on failure ensures consistency
  - No partial trades possible

- **Card Shop with Stock Management**:
  - Atomic stock decrement with database locks
  - Transaction-safe coin deduction
  - Card collection serialization to JSON
  - Prevents overselling

- **Leader Election**:
  - Redis-based distributed leader election
  - Scheduled tasks run only on leader
  - Automatic re-election on failure

- **Server Discovery & Registration**:
  - Servers automatically register with peers
  - Health checks and heartbeats
  - Dynamic cluster membership

### ✅ Real-Time Communication

- **WebSocket Protocol**: Bidirectional client-server communication
- **Redis Pub/Sub**: Cross-server event propagation
- **Event-Driven Architecture**: Async notifications for trades, matches, etc.
- **Anonymous Sessions**: Support for unauthenticated test clients

### ✅ Data Persistence & Consistency

- **JPA/Hibernate**: ORM for database interactions
- **PostgreSQL**: ACID-compliant transactions
- **JSON Serialization**: Complex card collections stored as JSONB
- **Optimistic Locking**: Prevent concurrent modification conflicts
- **Database Migrations**: Version-controlled schema changes

### ✅ High Availability & Fault Tolerance

- **Redis Sentinel**: Automatic master failover
- **Connection Pooling**: Efficient resource management
- **Retry Logic**: Graceful handling of transient failures
- **Health Checks**: Monitoring and alerting

### ✅ Performance Optimizations

- **Concurrent Data Structures**: Thread-safe queues and maps
- **Distributed Caching**: Redis for session and state caching
- **Lazy Loading**: JPA fetch strategies for optimal queries
- **Connection Reuse**: HTTP client pooling

## Technologies

### Core Technologies

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 21 | Programming language |
| Spring Boot | 3.2.0 | Application framework |
| Maven | 3.8+ | Dependency management & build |
| PostgreSQL | 16 | Relational database |
| Redis | 7.2 | Cache, Pub/Sub, distributed locks |
| Redisson | 3.25.0 | Redis client with advanced features |
| NGINX | latest | Reverse proxy & load balancer |

### Libraries & Frameworks

- **Spring Data JPA**: Database abstraction layer
- **Hibernate**: ORM implementation
- **Spring WebSocket**: WebSocket support
- **SLF4J/Logback**: Logging framework
- **Gson**: JSON serialization
- **Jackson**: JSON processing for REST APIs
- **HikariCP**: JDBC connection pooling
- **JUnit 5**: Unit testing framework

### DevOps & Infrastructure

- **Docker**: Containerization
- **Docker Compose**: Multi-container orchestration
- **Bash Scripts**: Automation and testing
- **PlantUML**: Architecture diagrams

## System Requirements

### Development

- **Java Development Kit (JDK)**: 21 or higher
- **Maven**: 3.8 or higher
- **Docker**: 20.10+ (optional but recommended)
- **Docker Compose**: 2.0+ (optional but recommended)
- **Git**: For version control

### Production

- **CPU**: 4+ cores recommended for multiple server instances
- **RAM**: 4GB minimum, 8GB recommended
- **Disk**: 10GB minimum (database + logs)
- **Network**: Low-latency connection for cross-server communication

### Operating Systems

- ✅ Linux (Ubuntu 20.04+, Debian 11+, Fedora, etc.)
- ✅ macOS (11+)
- ✅ Windows 10/11 (with WSL2 recommended)

## Installation

### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/dueling-protocol.git
cd dueling-protocol
```

### 2. Build with Maven

```bash
# Clean and build all modules
mvn clean package -DskipTests

# Or build with tests
mvn clean package
```

### 3. Build Docker Images (Optional)

```bash
# Build all Docker images
./scripts/build.sh

# Or use Docker Compose
cd docker
docker compose build
```

## Running the Project

### Full Distributed Setup

Start the complete distributed system with all infrastructure services:

```bash
# Navigate to docker directory
cd docker

# Start all services (PostgreSQL, Redis Sentinel, Gateway, Servers)
docker compose up -d

# Check status
docker compose ps

# View logs
docker compose logs -f
```

**Services Started:**
- PostgreSQL (port 5432)
- Redis Master (port 6379)
- Redis Slaves (ports 6380-6381)
- Redis Sentinels (ports 26379-26381)
- NGINX Gateway (port 80)
- Game Server 1 (port 8080)
- Game Server 2 (port 8083)

### Local Development

For faster development iterations without Docker:

```bash
# Terminal 1: Start infrastructure
cd docker
docker compose up postgres redis-master redis-sentinel-1 redis-sentinel-2 redis-sentinel-3 -d

# Terminal 2: Start Server 1
SERVER_PORT=8080 \
SERVER_NAME=server-1 \
POSTGRES_HOST=localhost \
POSTGRES_PORT=5432 \
REDIS_HOST=localhost \
REDIS_PORT=6379 \
java -Dspring.profiles.active=server,distributed,local-distributed \
     -jar dueling-server/target/dueling-server-1.0-SNAPSHOT.jar

# Terminal 3: Start Server 2
SERVER_PORT=8083 \
SERVER_NAME=server-2 \
POSTGRES_HOST=localhost \
POSTGRES_PORT=5432 \
REDIS_HOST=localhost \
REDIS_PORT=6379 \
java -Dspring.profiles.active=server,distributed,local-distributed \
     -jar dueling-server/target/dueling-server-1.0-SNAPSHOT.jar

# Terminal 4: Run tests
./test_scripts/test_cross_server_trade.sh
```

### Production Mode

For production deployment with monitoring and logging:

```bash
# Start with production profile
cd docker
docker compose --profile production up -d

# Enable detailed logging
export LOGGING_LEVEL=INFO

# Start servers with production settings
java -Xmx2G -Xms1G \
     -Dspring.profiles.active=server,distributed,production \
     -jar dueling-server/target/dueling-server-1.0-SNAPSHOT.jar
```

### Interactive Menu

Use the interactive menu for common operations:

```bash
./menu.sh
```

**Menu Options:**
1. Start infrastructure only
2. Start single server (development)
3. Start two servers (distributed testing)
4. Run cross-server trade test
5. Run cross-server match test
6. View server logs
7. Stop all services
8. Clean and rebuild
9. Database migrations
10. View system status
11. Exit

## Communication Protocol

### WebSocket Protocol

The system uses a custom text-based protocol over WebSocket:

```
GAME:{playerId}:{action}:{parameters...}
```

#### Character Setup
```
GAME:player123:CHARACTER_SETUP:HeroName:Human:Warrior
```

#### Matchmaking
```
GAME:player123:MATCHMAKING:ENTER
```

#### Card Purchase
```
GAME:player123:BUY_CARD:Fireball
```

#### Trade Proposal
```
GAME:player123:TRADE_PROPOSE:player456:Fireball:Ice Shard
```

#### Trade Response
```
GAME:player123:TRADE_ACCEPT:trade-id-123
GAME:player123:TRADE_REJECT:trade-id-123
```

### REST API Endpoints

#### Cross-Server Matchmaking

```http
GET /api/matchmaking/find-and-lock-partner
```

Response:
```json
{
  "id": "player123",
  "nickname": "Hero",
  "race": "Human",
  "class": "Warrior",
  "level": 5
}
```

#### Trade Proposal

```http
POST /api/trades/propose
Content-Type: application/json

{
  "proposingPlayerId": "player123",
  "targetPlayerId": "player456",
  "offeredCardIds": ["card-001", "card-002"],
  "requestedCardIds": ["card-101"]
}
```

Response:
```json
{
  "tradeId": "trade-123",
  "status": "PENDING"
}
```

#### Trade Acceptance

```http
POST /api/trades/{tradeId}/accept
```

Response:
```json
{
  "status": "Trade accepted and executed."
}
```

### Redis Pub/Sub Channels

```
# Player-specific channels
{playerId}:events

# Cross-server coordination
CROSS_SERVER:{playerId}

# System events
SYSTEM:matches
SYSTEM:trades
```

## Blockchain Verification

### Oracle Pattern Integrity Verification

The system records cryptographic proofs of all critical operations on the blockchain. Anyone can verify data integrity:

#### API Endpoints

```bash
# Health check
GET http://localhost:8080/api/verify/health

# Get verification instructions  
GET http://localhost:8080/api/verify/instructions

# Verify a purchase
POST http://localhost:8080/api/verify/purchase
Content-Type: application/json

{
  "purchaseId": "purchase-uuid",
  "operationType": "PURCHASE",
  "playerId": "player-id",
  "packType": "bronze",
  "coinsCost": 100,
  "timestamp": 1701900000000,
  "cardsReceived": [...]
}

# Verify a trade
POST http://localhost:8080/api/verify/trade

# Verify a match
POST http://localhost:8080/api/verify/match
```

#### Test Verification System

```bash
# Run automated Oracle Pattern tests
./test-oracle-pattern.sh
```

#### How It Works

1. **Operation Occurs**: Player buys pack, trades cards, or finishes match
2. **Hash Generation**: Backend calculates SHA-256 hash of operation data
3. **Blockchain Recording**: Hash stored in IntegrityContract (immutable)
4. **Public Verification**: Anyone can verify by providing same data
   - System recalculates hash
   - Compares with blockchain
   - Match = authentic ✅, Mismatch = tampered ❌

#### Benefits

- **Tamper-Evident**: Impossible to modify data without detection
- **Public Auditability**: Open API, no authentication required
- **Cryptographic Proof**: SHA-256 industry standard
- **Decentralized Truth**: Blockchain is ultimate authority

See [ORACLE_PATTERN_IMPLEMENTATION.md](ORACLE_PATTERN_IMPLEMENTATION.md) for complete documentation.

## Testing

### Automated Test Scripts

The project includes comprehensive test scripts:

```bash
# Cross-server card trade
./test_scripts/test_cross_server_trade.sh

# Cross-server matchmaking
./test_scripts/test_cross_server_match.sh

# Card purchase
./test_scripts/test_card_purchase.sh

# All tests
./test_scripts/run_all_tests.sh
```

### Manual Testing with WebSocket

Using `websocat`:

```bash
# Install websocat
cargo install websocat
# or: brew install websocat

# Connect to server
websocat ws://localhost:8080/ws

# Send commands
GAME:testPlayer:CHARACTER_SETUP:Hero:Human:Warrior
GAME:testPlayer:MATCHMAKING:ENTER
```

### Unit Tests

```bash
# Run all unit tests
mvn test

# Run specific test class
mvn test -Dtest=MatchmakingServiceTest

# Run with coverage
mvn clean test jacoco:report
```

### Integration Tests

```bash
# Run integration tests
mvn verify -P integration-tests

# Run specific integration test
mvn verify -Dtest=CrossServerMatchingIT
```

## Project Structure

```
dueling-protocol/
├── dueling-server/          # Main game server module
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   ├── config/             # Spring configurations
│   │   │   │   ├── controller/         # REST controllers & GameFacade
│   │   │   │   ├── model/              # Domain entities
│   │   │   │   ├── repository/         # JPA repositories
│   │   │   │   ├── service/            # Business logic
│   │   │   │   │   ├── matchmaking/    # Matchmaking services
│   │   │   │   │   ├── registry/       # Server registration
│   │   │   │   │   └── election/       # Leader election
│   │   │   │   ├── websocket/          # WebSocket handlers
│   │   │   │   └── pubsub/             # Redis Pub/Sub manager
│   │   │   └── resources/
│   │   │       ├── application.yml     # Base configuration
│   │   │       ├── application-server.yml
│   │   │       ├── application-distributed.yml
│   │   │       └── application-local-distributed.yml
│   │   └── test/                       # Unit tests
│   └── pom.xml
│
├── dueling-client/          # JavaFX client (GUI)
│   ├── src/
│   │   └── main/
│   │       └── java/
│   │           ├── view/               # UI controllers
│   │           ├── model/              # Client-side models
│   │           └── service/            # Client services
│   └── pom.xml
│
├── dueling-gateway/         # NGINX gateway configuration
│   ├── nginx.conf
│   └── Dockerfile.nginx
│
├── docker/                  # Docker Compose configurations
│   ├── docker-compose.yml   # Main compose file
│   ├── .env                 # Environment variables
│   └── init-db.sql          # Database initialization
│
├── scripts/                 # Automation scripts
│   ├── start_all_for_test.sh    # Start all services
│   ├── start_server1.sh          # Start server 1
│   ├── start_server2.sh          # Start server 2
│   ├── stop_all.sh               # Stop all services
│   └── build.sh                  # Build all modules
│
├── test_scripts/            # Test automation
│   ├── test_cross_server_trade.sh
│   ├── test_cross_server_match.sh
│   ├── test_card_purchase.sh
│   └── run_all_tests.sh
│
├── report/                  # Documentation & diagrams
│   ├── problem02/
│   │   ├── components.puml      # Component diagram
│   │   ├── components.svg
│   │   ├── sequence.puml        # Sequence diagram
│   │   └── sequence.svg
│   └── figuras/
│
├── menu.sh                  # Interactive menu
├── pom.xml                  # Parent POM
├── README.md                # This file
├── CHANGELOG_ORGANIZATION.md # Detailed change log
└── LICENSE
```

## Configuration

### Environment Variables

```bash
# Server Configuration
SERVER_PORT=8080              # Server port
SERVER_NAME=server-1          # Server instance name
PEER_SERVERS=http://server-2:8083  # Other servers

# Database Configuration
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
POSTGRES_DB=dueling_db
POSTGRES_USER=dueling_user
POSTGRES_PASSWORD=dueling_password

# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_SENTINEL_MASTER=redis-master
REDIS_SENTINEL_NODES=localhost:26379,localhost:26380,localhost:26381

# Spring Profiles
SPRING_PROFILES_ACTIVE=server,distributed,local-distributed

# Logging
LOGGING_LEVEL=INFO
LOGGING_PATTERN=%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n
```

### Spring Profiles

- **server**: Enable server-specific beans (vs gateway/client)
- **distributed**: Enable distributed features (Redis Pub/Sub, cross-server)
- **local-distributed**: Use single Redis instance (not Sentinel)
- **local-dev**: Simplified configuration for local development
- **production**: Production-grade settings (connection pools, timeouts)

### Application Configuration

See `dueling-server/src/main/resources/application-*.yml` for detailed configuration options.

## Troubleshooting

### Common Issues

#### 1. Port Already in Use

```bash
# Kill process using port 8080
lsof -ti:8080 | xargs kill -9

# Or use fuser
fuser -k 8080/tcp
```

#### 2. Redis Connection Refused

```bash
# Check Redis is running
docker compose ps redis-master

# Check Redis logs
docker compose logs redis-master

# Restart Redis
docker compose restart redis-master
```

#### 3. Database Connection Failed

```bash
# Check PostgreSQL is running
docker compose ps postgres

# Check credentials
docker compose exec postgres psql -U dueling_user -d dueling_db

# Reset database
docker compose down -v
docker compose up postgres -d
```

#### 4. WebSocket Connection Failed

```bash
# Check server logs
tail -f logs/server.log

# Test WebSocket with websocat
websocat ws://localhost:8080/ws

# Check NGINX is routing correctly
docker compose logs nginx
```

#### 5. Cross-Server Match Not Working

```bash
# Check both servers are registered
curl http://localhost:8080/api/health
curl http://localhost:8083/api/health

# Check server logs for [MATCH] entries
grep "\[MATCH\]" logs/*.log

# Verify cooldown mechanism
grep "cooldown" logs/*.log
```

### 6. Blockchain Connection Issues

```bash
# Check if blockchain node is running
curl -X POST -H "Content-Type: application/json" \
  --data '{"jsonrpc":"2.0","method":"eth_blockNumber","params":[],"id":1}' \
  http://localhost:8545

# Expected response: {"jsonrpc":"2.0","id":1,"result":"0x..."}

# Check blockchain service status
curl http://localhost:8080/api/blockchain/info

# Check for blockchain errors in logs
grep "Blockchain" logs/server.log | grep -i error

# Restart blockchain node
cd dueling-blockchain
npm run node
```

### 7. Smart Contract Deployment Failed

```bash
# Check deployment file exists
ls -la dueling-blockchain/deployment-info.json

# Verify contract addresses
cat dueling-blockchain/deployment-info.json

# Redeploy contracts
cd dueling-blockchain
npx hardhat clean
npm run compile
npm run deploy:local

# Restart server to reload contract addresses
```

### 8. Cards Not Appearing on Blockchain

```bash
# Check if blockchain is enabled
grep "blockchain.enabled" dueling-server/src/main/resources/application.yml

# Check BlockchainService logs
grep "BlockchainService" logs/server.log

# Verify card-token mapping
cat dueling-blockchain/card-token-mapping.json

# Check pending transactions
cd dueling-blockchain
node check_transactions.js

# Query blockchain directly
curl http://localhost:8080/api/blockchain/cards/0xYOUR_ADDRESS
```

### 9. Blockchain Event Listeners Not Working

```bash
# Check if listeners started
grep "Listening to.*events" logs/server.log

# Should see:
# "📡 Listening to CardMinted events"
# "📡 Listening to TradeAccepted events"
# "📡 Listening to MatchRecorded events"

# Check for event processing
grep "event:" logs/server.log

# Verify Web3j connection
grep "Web3j" logs/server.log | grep -i error
```

### 10. Transaction Nonce Issues

```bash
# Check for nonce errors
grep "nonce" logs/server.log | grep -i error

# Reset nonce (in blockchain node)
cd dueling-blockchain
npx hardhat clean
npm run node  # Restarts with fresh state

# Or manually reset pending transactions
# (Stop server, restart blockchain, restart server)
```

### 11. Gas Estimation Errors

```bash
# Check gas settings
grep "gas" logs/server.log | grep -i error

# Increase gas limit in BlockchainConfig
# Default: 6721975 (should be sufficient for local)

# For Sepolia testnet, ensure you have testnet ETH
# Get from: https://sepoliafaucet.com
```

### 12. Explorer Links Not Working

```bash
# Check network configuration
curl http://localhost:8080/api/blockchain/info | jq '.explorerUrl'

# For localhost: Should return local endpoint
# For Sepolia: Should return https://sepolia.etherscan.io

# Verify network ID
curl http://localhost:8080/api/blockchain/info | jq '.networkId'
# 1337 = Hardhat local
# 11155111 = Sepolia testnet
```

### Debug Mode

Enable debug logging:

```bash
# In application.yml
logging:
  level:
    controller: DEBUG
    service: DEBUG
    pubsub: DEBUG

# Or via environment variable
export LOGGING_LEVEL=DEBUG
```

### Health Checks

```bash
# Server health
curl http://localhost:8080/actuator/health

# Database health
docker compose exec postgres pg_isready

# Redis health
docker compose exec redis-master redis-cli ping
```

### Blockchain Troubleshooting

For comprehensive blockchain-specific troubleshooting, see:
📖 **[BLOCKCHAIN_TROUBLESHOOTING.md](BLOCKCHAIN_TROUBLESHOOTING.md)**

Quick blockchain checks:
```bash
# Check if blockchain is running
curl -X POST -H "Content-Type: application/json" \
  --data '{"jsonrpc":"2.0","method":"eth_blockNumber","params":[],"id":1}' \
  http://localhost:8545

# Check blockchain integration status
curl http://localhost:8080/api/blockchain/info

# View blockchain logs
grep "Blockchain" logs/server.log | tail -20

# Check event listeners
grep "Listening to.*events" logs/server.log
```

## Contributing

We welcome contributions! Please follow these guidelines:

### Development Workflow

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Commit your changes: `git commit -m 'feat: add amazing feature'`
4. Push to the branch: `git push origin feature/amazing-feature`
5. Open a Pull Request

### Commit Message Convention

Follow semantic commit messages:

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Types:**
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting)
- `refactor`: Code refactoring
- `test`: Adding/updating tests
- `chore`: Build process, dependencies, tooling

**Examples:**
```
feat(matchmaking): add cooldown mechanism to prevent race conditions
fix(trade): ensure atomic rollback on 2PC failure
docs(readme): add architecture diagrams
refactor(service): extract common logic to base class
test(integration): add cross-server trade test
```

### Code Style

- Follow Java naming conventions
- Use meaningful variable names
- Add JavaDoc for public methods
- Keep methods small and focused
- Write unit tests for new features

### Testing Requirements

- All new features must have unit tests
- Integration tests for cross-server features
- Test coverage should not decrease

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 📚 Additional Resources

- [Architecture Decision Records](docs/adr/)
- [API Documentation](docs/api/)
- [Deployment Guide](docs/deployment/)
- [Performance Tuning](docs/performance/)
- [Security Best Practices](docs/security/)

## 🤝 Support

- 📧 Email: support@duelingprotocol.com
- 💬 Discord: [Join our server](https://discord.gg/duelingprotocol)
- 🐛 Issues: [GitHub Issues](https://github.com/yourusername/dueling-protocol/issues)
- 📖 Wiki: [Project Wiki](https://github.com/yourusername/dueling-protocol/wiki)

## 🙏 Acknowledgments

- Spring Framework team for excellent documentation
- Redis team for powerful distributed primitives
- PlantUML for architecture diagram generation
- The open-source community for inspiration and tools

---

**Made with ❤️ by the Dueling Protocol Team**
