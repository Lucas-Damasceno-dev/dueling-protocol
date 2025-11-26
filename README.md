# Dueling Protocol

A distributed multiplayer card game system built with microservices architecture, featuring cross-server matchmaking, atomic card trading using distributed locks, and real-time WebSocket communication.

## 📑 Table of Contents

- [Overview](#overview)
- [Distributed Architecture](#distributed-architecture)
  - [Component Diagram](#component-diagram)
  - [Sequence Diagram](#sequence-diagram)
- [Key Features](#key-features)
- [Technologies](#technologies)
- [System Requirements](#system-requirements)
- [Installation](#installation)
- [Running the Project](#running-the-project)
  - [Full Distributed Setup](#full-distributed-setup)
  - [Local Development](#local-development)
  - [Production Mode](#production-mode)
- [Communication Protocol](#communication-protocol)
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
4. **`dueling-blockchain`**: Manages digital assets (cards) as NFTs on a local Ethereum blockchain using Solidity smart contracts.
5. **Infrastructure Services**:
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
| NGINX      | latest  | Reverse proxy & load balancer |
| Node.js    | 18+     | Blockchain Environment        |
| Hardhat    | 2.x     | Ethereum Development Framework|
| Solidity   | 0.8.x   | Smart Contract Language       |

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

### 3. Install Blockchain Dependencies

Navigate to the blockchain directory and install the necessary npm packages. This is required for the Docker build and for running local tests.

```bash
cd dueling-blockchain
npm install
cd ..
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

### Using Docker (Recommended Method)

The recommended way to run the entire system is by using the provided shell script. This script handles building the images, starting all services in the correct order, and deploying the necessary smart contracts to the local blockchain.

```bash
# Make sure the script is executable
chmod +x scripts/start-complete-with-blockchain.sh

# Run the script
./scripts/start-complete-with-blockchain.sh
```

This will start all services, including:
- NGINX Gateway (port 8080)
- Multiple Game Servers
- PostgreSQL Database
- Redis Sentinel Cluster
- Dueling-Blockchain Node (port 8545)
- Prometheus & Grafana for monitoring

To stop the system, use the corresponding stop script:
```bash
./scripts/stop-all-with-blockchain.sh
```

### Local Development (Advanced)

For faster development iterations without Docker for the Java services, you can run the infrastructure in Docker and the servers locally.

```bash
# Terminal 1: Start infrastructure (DB, Redis, Blockchain)
cd docker
docker compose up -d postgres redis-master redis-sentinel-1 redis-sentinel-2 redis-sentinel-3 dueling-blockchain

# Terminal 2: Start Server 1
./scripts/run_server.sh 8081 server-1

# Terminal 3: Start Server 2
./scripts/run_server.sh 8082 server-2
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

### Blockchain Tests

Run the tests for the Solidity smart contracts using Hardhat.

```bash
cd dueling-blockchain
npx hardhat test
cd ..
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
