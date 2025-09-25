import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.UUID;

public class GameClient {
    private static final String SERVER_ADDRESS = System.getenv().getOrDefault("SERVER_HOST", "127.0.0.1");
    private static final int TCP_PORT = 7777;
    private static final int UDP_PORT = 7778;
    
    private static Socket socket;
    private static PrintWriter out;
    private static String playerId;
    private static String currentMatchId;
    private static boolean inGame = false;
    
    public static void main(String[] args) {
        try {
            socket = new Socket(SERVER_ADDRESS, TCP_PORT);
            System.out.println("Conectado ao servidor em " + SERVER_ADDRESS + ":" + TCP_PORT);

            Thread receiverThread = new Thread(new ServerMessageReceiver(socket));
            receiverThread.start();

            out = new PrintWriter(socket.getOutputStream(), true);

            playerId = "player-" + UUID.randomUUID().toString().substring(0, 8);
            System.out.println(">> Seu ID de jogador é: " + playerId);

            // Modos automáticos para testes
            if (args.length > 0) {
                switch (args[0]) {
                    case "autobot":
                        runAutobot(args.length > 1 ? args[1] : "");
                        return;
                    case "maliciousbot":
                        runMaliciousBot();
                        return;
                }
            }

            // Menu interativo
            showMenu();

        } catch (Exception e) {
            System.err.println("Erro ao conectar ao servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }
    // Modo autobot para testes de desconexão abrupta e concorrência
    private static void runAutobot(String scenario) {
        try {
            out.println("CHARACTER_SETUP:" + playerId + ":Elfo:Guerreiro");
            Thread.sleep(200);

            switch (scenario) {
                case "matchmaking_disconnect":
                    scenarioMatchmakingDisconnect();
                    return;
                case "pre_game_disconnect":
                    scenarioPreGameDisconnect();
                    return;
                case "simultaneous_play":
                    scenarioSimultaneousPlay();
                    return;
                case "mid_game_disconnect":
                    scenarioMidGameDisconnect();
                    return;
                case "race_condition":
                    scenarioRaceCondition();
                    return;
                default:
                    scenarioDefault();
                    return;
            }
        } catch (Exception e) {
            System.err.println("[autobot] Erro: " + e.getMessage());
        }
    }

    // Cada cenário extraído para métodos privados
    private static void scenarioMatchmakingDisconnect() throws Exception {
        out.println("MATCHMAKING:" + playerId + ":ENTER");
        Thread.sleep(500);
        System.out.println("[autobot] Desconectando abruptamente na fila de matchmaking");
        socket.close();
    }

    private static void scenarioPreGameDisconnect() throws Exception {
        out.println("MATCHMAKING:" + playerId + ":ENTER");
        int waited = 0;
        while (!inGame && waited < 10000) {
            Thread.sleep(200);
            waited += 200;
        }
        if (!inGame) {
            System.out.println("[autobot] Desconectando abruptamente antes do GAME_START");
            socket.close();
        }
    }

    private static void scenarioSimultaneousPlay() throws Exception {
        out.println("MATCHMAKING:" + playerId + ":ENTER");
        int waited = 0;
        while (!inGame && waited < 10000) {
            Thread.sleep(200);
            waited += 200;
        }
        if (inGame) {
            out.println("GAME:" + playerId + ":" + currentMatchId + ":PLAY_CARD:card-001");
            System.out.println("[autobot] Jogada enviada imediatamente após GAME_START");
            Thread.sleep(500);
            socket.close();
        }
    }

    private static void scenarioMidGameDisconnect() throws Exception {
        out.println("MATCHMAKING:" + playerId + ":ENTER");
        int waited = 0;
        while (!inGame && waited < 10000) {
            Thread.sleep(200);
            waited += 200;
        }
        if (inGame) {
            out.println("GAME:" + playerId + ":" + currentMatchId + ":PLAY_CARD:card-001");
            Thread.sleep(300);
            System.out.println("[autobot] Desconectando abruptamente no meio da partida");
            socket.close();
        }
    }

    private static void scenarioRaceCondition() throws Exception {
        out.println("STORE:" + playerId + ":BUY:BASIC");
        out.println("UPGRADE:" + playerId + ":BASE_ATTACK");
        Thread.sleep(500);
        socket.close();
    }

    private static void scenarioDefault() throws Exception {
        System.out.println("[autobot] Teste concluído, saindo...");
        socket.close();
    }

    // Modo maliciousbot para enviar mensagens malformadas
    private static void runMaliciousBot() {
        try {
            // Comando incompleto
            out.println("MATCHMAKING:" + playerId);
            Thread.sleep(200);
            // Comando com parâmetro extra
            out.println("STORE:" + playerId + ":BUY:BASIC:EXTRA_PARAM");
            Thread.sleep(200);
            // Comando fora de contexto
            out.println("GAME:PLAY_CARD:card-001");
            Thread.sleep(200);
            // Comando totalmente inválido
            out.println("INVALIDCOMMAND");
            Thread.sleep(200);
            System.out.println("[maliciousbot] Mensagens malformadas enviadas. Saindo...");
            socket.close();
        } catch (Exception e) {
            System.err.println("[maliciousbot] Erro: " + e.getMessage());
        }
    }
    
    private static void showMenu() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("\n=== MENU DO JOGO ===");
            System.out.println("1. Configurar personagem");
            System.out.println("2. Entrar na fila de matchmaking");
            System.out.println("3. Comprar pacote de cards");
            System.out.println("4. Verificar ping");
            System.out.println("5. Jogar carta (durante partida)");
            System.out.println("6. Melhorar atributos");
            System.out.println("7. Sair");
            System.out.print("Escolha uma opção: ");
            
            String input = scanner.nextLine().trim();
            
            switch (input) {
                case "1":
                    setupCharacter(scanner);
                    break;
                case "2":
                    enterMatchmaking();
                    break;
                case "3":
                    buyCardPack(scanner);
                    break;
                case "4":
                    checkPing();
                    break;
                case "5":
                    if (inGame) {
                        playCard(scanner);
                    } else {
                        System.out.println("Você precisa estar em uma partida para jogar cartas!");
                    }
                    break;
                case "6":
                    upgradeAttributes(scanner);
                    break;
                case "7":
                    System.out.println("Saindo...");
                    try {
                        socket.close();
                    } catch (IOException e) {
                        System.err.println("Erro ao fechar conexão: " + e.getMessage());
                    }
                    return;
                default:
                    System.out.println("Opção inválida!");
                    break;
            }
        }
    }
    
    private static void setupCharacter(Scanner scanner) {
        System.out.println("\n=== CONFIGURAÇÃO DE PERSONAGEM ===");
        System.out.println("Raças disponíveis: Elfo, Anão, Humano, Orc");
        System.out.print("Escolha sua raça: ");
        String race = scanner.nextLine().trim();
        
        System.out.println("Classes disponíveis: Guerreiro, Mago, Arqueiro, Ladino");
        System.out.print("Escolha sua classe: ");
        String playerClass = scanner.nextLine().trim();
        
        out.println("CHARACTER_SETUP:" + playerId + ":" + race + ":" + playerClass);
    }
    
    private static void enterMatchmaking() {
        System.out.println("Entrando na fila de matchmaking...");
        out.println("MATCHMAKING:" + playerId + ":ENTER");
    }
    
    private static void buyCardPack(Scanner scanner) {
        System.out.println("\n=== LOJA DE CARDS ===");
        System.out.println("Tipos de pacotes: BASIC, PREMIUM, LEGENDARY");
        System.out.print("Qual pacote deseja comprar? ");
        String packType = scanner.nextLine().trim().toUpperCase();
        
        out.println("STORE:" + playerId + ":BUY:" + packType);
    }
    
    private static void playCard(Scanner scanner) {
        System.out.print("ID da carta a ser jogada: ");
        String cardId = scanner.nextLine().trim();
        
        out.println("GAME:" + playerId + ":" + currentMatchId + ":PLAY_CARD:" + cardId);
    }
    
    private static void upgradeAttributes(Scanner scanner) {
        System.out.println("\n=== MELHORIA DE ATRIBUTOS ===");
        System.out.println("Atributos disponíveis para melhoria:");
        System.out.println("BASE_ATTACK - Custo: 5 pontos");
        System.out.print("Qual atributo deseja melhorar? ");
        String attribute = scanner.nextLine().trim().toUpperCase();
        
        out.println("UPGRADE:" + playerId + ":" + attribute);
    }
    
    public static void checkPing() {
        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress address = InetAddress.getByName(SERVER_ADDRESS);
            
            long startTime = System.currentTimeMillis();
            String message = String.valueOf(startTime);
            byte[] buffer = message.getBytes();

            DatagramPacket request = new DatagramPacket(buffer, buffer.length, address, UDP_PORT);
            socket.send(request);
            
            DatagramPacket response = new DatagramPacket(new byte[buffer.length], buffer.length);
            socket.setSoTimeout(1000);
            socket.receive(response);

            long endTime = System.currentTimeMillis();
            long latency = endTime - startTime;
            System.out.println("Ping: " + latency + " ms");

        } catch (IOException e) {
            System.err.println("Ping falhou: " + e.getMessage());
        }
    }
    
    static class ServerMessageReceiver implements Runnable {
        private Socket socket;
        
        public ServerMessageReceiver(Socket socket) {
            this.socket = socket;
        }
        
        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                String response;
                while ((response = in.readLine()) != null) {
                    processServerMessage(response);
                }
            } catch (IOException e) {
                System.err.println("Erro ao receber mensagem do servidor: " + e.getMessage());
            }
        }
        
        private void processServerMessage(String message) {
            String[] parts = message.split(":");
            String type = parts[0];

            if ("UPDATE".equals(type)) {
                String subType = parts[1];
                switch (subType) {
                    case "GAME_START":
                        currentMatchId = parts[2];
                        inGame = true;
                        System.out.println("\n>> Partida encontrada contra: " + parts[3]);
                        System.out.println(">> ID da partida: " + currentMatchId);
                        break;
                    case "ACTION":
                        System.out.println("\n>> " + parts[2]);
                        break;
                    case "HEALTH":
                        System.out.println("\n>> Vida atualizada: " + parts[2] + " agora tem " + parts[3] + " pontos de vida");
                        break;
                    case "GAME_OVER":
                        inGame = false;
                        currentMatchId = null;
                        System.out.println("\n>> FIM DE JOGO: Você " + (parts[2].equals("VICTORY") ? "VENCEU!" : "PERDEU!"));
                        break;
                    case "DRAW_CARDS":
                        System.out.println("\n>> Cartas recebidas: " + parts[2]);
                        break;
                    default:
                        System.out.println("\nServidor: " + message);
                        break;
                }
            } else if ("SUCCESS".equals(type)) {
                System.out.println("\n✓ " + message.substring(8)); // Remove "SUCCESS:" prefix
            } else if ("ERROR".equals(type)) {
                System.out.println("\n✗ Erro: " + message.substring(6)); // Remove "ERROR:" prefix
            } else {
                System.out.println("\nServidor: " + message);
            }
        }
    }
}