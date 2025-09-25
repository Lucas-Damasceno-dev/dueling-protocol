# Dueling Protocol

Uma implementação de servidor para um jogo de cartas multiplayer, construído com uma arquitetura cliente-servidor utilizando comunicação TCP.

## Arquitetura

O projeto segue um modelo cliente-servidor clássico:

-   **Servidor**: Uma aplicação Java que gerencia a lógica central do jogo, incluindo matchmaking, sessões de partida, compra de pacotes de cartas e persistência de dados dos jogadores.
-   **Clientes**: Qualquer aplicação (desktop, mobile, web) capaz de se comunicar via sockets TCP. O projeto inclui um `GameClient` como uma implementação de exemplo.

## Barema de Funcionalidades

Esta seção detalha como o projeto atende aos critérios de avaliação propostos.

### 1. Comunicação e API Remota

A comunicação entre cliente e servidor é baseada em texto e utiliza TCP para ações críticas (lógica de jogo, matchmaking, etc.) e UDP para medição de latência.

**Formato Geral (TCP):** `COMANDO:ID_JOGADOR:ARG1:ARG2...`

**Comandos Principais:**

| Comando | Formato | Descrição |
| :--- | :--- | :--- |
| `CHARACTER_SETUP` | `CHARACTER_SETUP:<playerId>:<race>:<class>` | Define a raça e a classe do personagem do jogador. |
| `MATCHMAKING` | `MATCHMAKING:<playerId>` | Adiciona o jogador à fila para encontrar uma partida. |
| `STORE` | `STORE:<playerId>:BUY:<packType>` | Compra um pacote de cartas de um tipo específico. |
| `GAME` | `GAME:<playerId>:PLAY_CARD:<matchId>:<cardId>` | Executa a ação de jogar uma carta durante uma partida. |
| `UPGRADE` | `UPGRADE:<playerId>:<attribute>` | Melhora um atributo do jogador usando pontos de melhoria. |

**Respostas do Servidor:**

O servidor responde com prefixos que indicam o resultado da operação:

-   `SUCCESS`: Ação executada com sucesso.
-   `ERROR`: Ocorreu um erro ao processar o comando.
-   `UPDATE`: Envia uma atualização assíncrona para o cliente (início de partida, compra de cartas, etc.).

### 2. Encapsulamento e Formato de Dados

Os dados são encapsulados em mensagens de texto simples. O servidor faz o parsing dessas mensagens para extrair comandos e argumentos. Em caso de formato inválido, o servidor responde com uma mensagem de `ERROR`.

### 3. Concorrência

O servidor é multithread, utilizando um modelo de uma thread por cliente (`ClientHandler`). Cada conexão de cliente é gerenciada em sua própria thread, permitindo o processamento simultâneo de múltiplas requisições.

As estruturas de dados compartilhadas (ex: listas de jogos ativos, clientes conectados e a fila de matchmaking) utilizam implementações `thread-safe` (como `ConcurrentHashMap` e `ConcurrentLinkedQueue` no `ConcurrentMatchmakingService`) para garantir a consistência e evitar condições de corrida.

### 4. Partidas 1v1

O sistema de matchmaking permite que jogadores entrem em uma fila. O `ConcurrentMatchmakingService` processa a fila continuamente e, quando encontra dois jogadores, cria uma partida (`Match`).

O sistema verifica se os dois jogadores ainda estão conectados antes de iniciar a partida. Se um jogador desconectar, o outro é devolvido à fila, garantindo que ninguém fique "preso" esperando por um oponente que já saiu.

### 5. Pacotes de Cartas

A aquisição de novas cartas é feita através da compra de pacotes na loja. O comando `STORE:BUY` aciona essa lógica. O `StoreServiceImpl` é responsável por determinar quais cartas são entregues ao jogador, simulando um "estoque" global de onde as cartas são sorteadas.

### 6. Latência

Para garantir uma experiência de jogo responsiva, o sistema oferece um mecanismo para medir a latência (ping) entre cliente e servidor. Isso é feito através do `PingServer`, que opera em uma porta dedicada (7778) utilizando o protocolo UDP.

-   **Funcionamento**: O `PingServer` é um servidor de eco. O cliente envia um pacote UDP para o servidor, que o devolve imediatamente.
-   **Visualização do Atraso**: O cliente pode medir o tempo de ida e volta (Round-Trip Time) e exibir essa informação na interface, permitindo que o jogador visualize a qualidade da sua conexão.

### 7. Testes e Emulação

O projeto inclui um script de teste de estresse (`stress_test.sh`) que atende aos critérios de automação e emulação:

-   **Emulação com Docker**: O teste utiliza `docker-compose` para orquestrar um ambiente com múltiplas instâncias do cliente e um servidor, simulando um cenário de uso real.
-   **Teste de Estresse Automatizado**: O script inicia 10 clientes (`BOT_MODE=autobot`) que se conectam e interagem com o servidor simultaneamente. O teste executa por um período fixo (30 segundos) e depois encerra e limpa os contêineres automaticamente, permitindo uma avaliação de desempenho e estabilidade sob carga.

## Como Construir e Executar

### Com Maven

```bash
# Compila o projeto e gera o JAR
./build.sh
```

### Com Docker

```bash
# Constrói a imagem Docker
docker-compose build

# Inicia o servidor
docker-compose up server
```

### Executando os Testes

Para executar o teste de estresse automatizado:

```bash
./stress_test.sh
```
