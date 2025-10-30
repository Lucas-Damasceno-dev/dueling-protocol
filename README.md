# Dueling Protocol

A server implementation for a multiplayer card game, built with a client-server architecture using TCP communication.

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Features](#features)
- [Technologies](#technologies)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Running the Project](#running-the-project)
  - [With Docker](#with-docker)
  - [With Maven](#with-maven)
  - [Locally (Development)](#locally-development)
- [Communication Protocol](#communication-protocol)
- [Testing](#testing)
- [Project Structure](#project-structure)
- [Contributing](#contributing)
- [License](#license)

## Overview

Dueling Protocol is a server for a multiplayer card game that allows the creation of 1v1 matches between players. The server manages the entire game lifecycle, from matchmaking to match resolution, and also offers features like a card shop, character upgrade system, and latency measurement.

## Architecture

The project follows a microservices architecture with a gateway pattern:

- **API Gateway**: An NGINX reverse proxy (replacing Spring Cloud Gateway) that acts as a reverse proxy, routing requests to the appropriate services and handling cross-cutting concerns like authentication and rate limiting. This change was made to resolve critical WebSocket connection issues with Spring Cloud Gateway.
- **Game Server**: A Java application that manages the core game logic, including matchmaking, match sessions, card pack purchases, and player data persistence.
- **Game Client**: A command-line client that connects to the gateway to interact with the game services.
- **Database**: PostgreSQL for persistent data storage.
- **Cache**: Redis for session management, distributed locks, and caching.

![Architecture Diagram](report/figuras/arquitetura.png)

## Features

- ✅ **Microservices Architecture**: Modular design with clear separation of concerns
- ✅ **API Gateway**: Centralized entry point with authentication and routing
- ✅ **JWT Authentication**: Secure token-based authentication
- ✅ **1v1 Matchmaking**: Queue system to pair players
- ✅ **1v1 Matches**: Complete match system between two players
- ✅ **Card Shop**: Purchase card packs to expand the deck
- ✅ **Upgrade System**: Improve character attributes with points earned from victories
- ✅ **Data Persistence**: Storage of player data in PostgreSQL database
- ✅ **Caching**: Redis-based caching for improved performance
- ✅ **WebSocket Communication**: Real-time communication for game updates
- ✅ **Thread-Safe Concurrency**: Multithreaded architecture with safe data structures
- ✅ **Latency Measurement**: Ping system to monitor connection quality
- ✅ **Disconnection Handling**: Management of abrupt client disconnections

## Technologies

- Java 21
- Maven
- Docker
- Docker Compose
- Spring Boot
- NGINX (as reverse proxy, replacing Spring Cloud Gateway)
- PostgreSQL
- Redis (with Redisson for distributed locks)
- JWT (JSON Web Tokens)
- WebSocket
- TCP/UDP Sockets
- Gson (for JSON serialization)
- SLF4J/Logback (for logging)

## Prerequisites

- Java 21
- Maven 3.8+
- Docker (optional, but recommended)
- Docker Compose (optional, but recommended)

## Installation

### With Docker (Recommended)

```bash
# Clone the repository
git clone <repository-url>
cd dueling-protocol

# Build the Docker images
./build.sh
```

### With Maven

```bash
# Clone the repository
git clone <repository-url>
cd dueling-protocol

# Compile the project
mvn clean package
```

## Running the Project

### Quick Start with Menu System

For an easy-to-use interface with all available options:

```bash
# Run the main menu
./menu.sh
```

This provides access to all system functions including starting, testing, monitoring, and distributed deployment.

### With Docker

```bash
# Start all services with Docker Compose
cd docker && docker compose up -d

# Or start individual services
./run_server.sh
./run_gateway.sh
./run_client.sh
```

### With Maven

```bash
# Start infrastructure services (PostgreSQL and Redis)
cd docker && docker compose up postgres redis-master redis-slave redis-sentinel-1 redis-sentinel-2 redis-sentinel-3 -d

# Start the gateway
cd ../dueling-gateway
mvn spring-boot:run

# In another terminal, start the server
cd ../dueling-server
mvn spring-boot:run

# In another terminal, start the client
cd ../dueling-client
mvn exec:java -Dexec.mainClass="client.GameClient"
```

### Locally (Development)

For local development without Docker, you can use the simple startup scripts:

1. **Easy Method (Recommended)**:
   ```bash
   # Use one of the simplified startup scripts:
   ./start_game_local.sh          # Standard local startup
   ./start_game_local_simple.sh   # With simple Redis (no Sentinel)
   ./start_game_redis_disabled.sh # With Redis disabled
   ```

2. **Manual Method**:
   ```bash
   # Start infrastructure services
   cd docker && docker compose up postgres redis-master -d
   
   # In one terminal, start the gateway
   ./start_game_redis_disabled.sh
   
   # In another terminal, start the server manually
   cd dueling-server
   mvn spring-boot:run -Dspring.profiles.active=local-dev
   ```

3. **Run the Client**:
   ```bash
   # In another terminal, run the client
   ./run_client.sh
   ```

Refer to [INSTRUCOES_EXECUCAO.md](INSTRUCOES_EXECUCAO.md) for detailed instructions on running the project locally.

## Communication Protocol

The communication between client and server uses a combination of HTTP/REST for authentication and WebSocket for real-time gameplay.

### Authentication Flow

1. Client registers with `/api/auth/register`
2. Client authenticates with `/api/auth/login` to receive a JWT token
3. Client uses the JWT token in the `Authorization: Bearer <token>` header for all subsequent requests

### WebSocket Communication

After authentication, the client establishes a WebSocket connection to `/ws` with the JWT token as a query parameter.

### REST API Endpoints

| Endpoint | Method | Description |
| :--- | :--- | :--- |
| `/api/auth/register` | POST | Registers a new user |
| `/api/auth/login` | POST | Authenticates a user and returns a JWT token |
| `/api/friends` | GET | Retrieves the list of friends for the authenticated user |
| `/api/friends/request` | POST | Sends a friend request to another user |
| `/api/friends/accept` | POST | Accepts a pending friend request |
| `/api/friends/reject` | POST | Rejects a pending friend request |
| `/api/match/create` | POST | Creates a new match |
| `/api/move/play` | POST | Plays a card during a match |
| `/api/state` | GET | Retrieves the current game state |
| `/api/user/profile` | GET | Retrieves the authenticated user's profile |
| `/api/user/stats` | GET | Retrieves the authenticated user's statistics |

## Testing

The project includes a complete suite of automated tests that cover various scenarios:

### Unit Tests

```bash
# Run all unit tests
mvn test
```

### Integration Tests

```bash
# Run integration tests
mvn verify
```

### Scenario Tests

```bash
# Run all scenario tests
./run_all_tests.sh
```

### Stress Test

```bash
# Run a stress test with 10 simultaneous clients
./test_scripts/test_stress.sh
```

The tests cover:
- Authentication flows
- Friend management operations
- Matchmaking scenarios
- Game play sequences
- Abrupt disconnections during matchmaking and matches
- Race conditions in data persistence
- Simultaneous moves
- Handling of malformed inputs

## Project Structure

```
dueling-protocol/
├── docker/              # Docker configurations and compose files
├── dueling-client/      # Command-line game client
├── dueling-gateway/     # API Gateway implementation
├── dueling-server/      # Main game server implementation
├── scripts/             # Utility scripts for deployment and testing
├── test_scripts/        # Automated test scripts
├── report/              # Documentation and diagrams
├── pom.xml              # Maven parent project configuration
├── README.md            # Project documentation
├── INSTRUCOES_EXECUCAO.md # Detailed execution instructions
└── RESOLUCAO_PROBLEMAS.md # Problem resolution documentation
```

## Contributing

1. Fork the project
2. Create a branch for your feature (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## License

Distributed under the MIT License. See `LICENSE` for more information.
## Project Structure Update

All scripts have been organized into a clean structure with only `menu.sh` in the root directory as the main entry point:

- **`menu.sh`** - Main menu interface (only script in root directory)
- **`scripts/`** directory with organized subdirectories:
  - `build/` - Build and test execution scripts
  - `run/` - Runtime execution scripts
  - `deploy/` - Deployment and startup scripts  
  - `monitor/` - Monitoring scripts
- **`test_scripts/`** - Organized test scripts (functional, integration, performance, etc.)

This structure makes the project cleaner and easier to navigate, with `./menu.sh` providing access to all functionality.