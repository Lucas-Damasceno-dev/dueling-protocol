# Dueling Protocol

A card game server implementation with TCP/UDP networking.

## Architecture

The project implements a client-server architecture where:

- **Server**: Manages matchmaking, ongoing matches, card pack purchases, and player data storage.
- **Clients**: Can be desktop, mobile, or web applications that render the game UI and communicate with the server.

## Networking

- **TCP (Port 7777)**: Used for reliable game actions like matchmaking and purchasing card packs.
- **UDP (Port 7778)**: Used for ping measurements to check latency.

## Building the Project

### With Maven

```bash
mvn clean package
```

### With Docker

```bash
docker build -t dueling-protocol .
docker run -p 7777:7777/tcp -p 7778:7778/udp dueling-protocol
```

## Running the Server

```bash
java -jar target/dueling-protocol-1.0-SNAPSHOT.jar
```

## Components

- `GameServer`: Main server class that handles TCP connections and starts the UDP ping server.
- `ClientHandler`: Handles communication with individual clients.
- `PingServer`: UDP server for latency measurements.
- `GameClient`: Sample client implementation.