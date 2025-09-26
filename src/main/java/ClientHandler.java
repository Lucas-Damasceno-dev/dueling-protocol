import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Optional;
import controller.GameFacade;
import model.Player;
import repository.PlayerRepository;
import repository.PlayerRepositoryJson;
import service.store.PurchaseResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final GameFacade gameFacade;
    private final PlayerRepository playerRepository;
    private Player player; // Armazena o jogador desta sessão
    
    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);

    public ClientHandler(Socket socket, GameFacade facade) {
        this.clientSocket = socket;
        this.gameFacade = facade;
        this.playerRepository = new PlayerRepositoryJson();
    }

    @Override
    public void run() {
        try (
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))
        ) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                logger.debug("Comando recebido: {}", inputLine);
                String[] command = inputLine.split(":");
                processCommand(command, out);
            }
        } catch (Exception e) {
            logger.warn("Conexão com o cliente perdida: {}", e.getMessage(), e);
        } finally {
            // Garante que o cliente seja removido ao desconectar
            if (player != null) {
                gameFacade.removeClient(player.getId());
                logger.info("Cliente {} desconectado", player.getId());
            }
        }
    }

    private void processCommand(String[] command, PrintWriter out) {
        if (command.length < 2) {
            logger.warn("Comando inválido recebido: {}", String.join(":", command));
            out.println("ERROR:Comando inválido.");
            return;
        }
        
        String action = command[0];
        String playerId = command[1];

        if (playerId == null || playerId.trim().isEmpty()) {
            logger.warn("ID do jogador não fornecido para ação: {}", action);
            out.println("ERROR:ID do jogador não fornecido.");
            return;
        }

        // Carrega o jogador ou cria um novo, e o registra
        if (this.player == null) {
            this.player = getOrCreatePlayer(playerId);
            gameFacade.registerClient(playerId, out);
        }

        Player player = getOrCreatePlayer(playerId);

        switch (action) {
            case "CHARACTER_SETUP":
                if (command.length < 4) {
                    logger.warn("Comando CHARACTER_SETUP incompleto");
                    out.println("ERROR:Comando CHARACTER_SETUP incompleto. Use: CHARACTER_SETUP:<playerId>:<race>:<class>");
                    return;
                }
                String race = command[2];
                String pClass = command[3];
                player.setCharacter(race, pClass);
                playerRepository.save(player);
                logger.info("Personagem configurado para {}: {} {}", playerId, race, pClass);
                out.println("SUCCESS:Personagem configurado como " + race + " " + pClass);
                break;

            case "MATCHMAKING":
                gameFacade.enterMatchmaking(player);
                logger.info("Jogador {} entrou na fila de matchmaking", playerId);
                out.println("SUCCESS:Você entrou na fila.");
                break;

            case "STORE":
                if (command.length < 4) {
                    logger.warn("Comando STORE incompleto");
                    out.println("ERROR:Comando STORE incompleto. Use: STORE:<playerId>:BUY:<packType>");
                    return;
                }
                String packType = command[3];
                PurchaseResult result = gameFacade.buyPack(player, packType);

                if (result.isSuccess()) {
                    playerRepository.save(player); // Garante que o estado do jogador (moedas, cartas) seja salvo
                    StringBuilder cardsStr = new StringBuilder();
                    result.getCards().forEach(c -> cardsStr.append(c.getName()).append(" (").append(c.getRarity()).append("), "));
                    String cardList = cardsStr.length() > 0 ? cardsStr.substring(0, cardsStr.length() - 2) : "Nenhuma";
                    
                    logger.info("Jogador {} comprou pacote do tipo {} com sucesso.", playerId, packType);
                    out.println("SUCCESS:Pacote comprado! Cartas obtidas: " + cardList);
                } else {
                    String errorMessage;
                    switch (result.getStatus()) {
                        case INSUFFICIENT_FUNDS:
                            errorMessage = "Moedas insuficientes.";
                            break;
                        case OUT_OF_STOCK:
                            errorMessage = "Pacote esgotado.";
                            break;
                        case PACK_NOT_FOUND:
                        default:
                            errorMessage = "Erro na compra do pacote.";
                            break;
                    }
                    logger.warn("Falha na compra do pacote {} para o jogador {}: {}", packType, playerId, errorMessage);
                    out.println("ERROR:" + errorMessage);
                }
                break;

            case "GAME":
                gameFacade.processGameCommand(command, out);
                break;
            
            case "UPGRADE":
                if (command.length < 3) {
                    logger.warn("Comando UPGRADE incompleto");
                    out.println("ERROR:Comando UPGRADE incompleto. Use: UPGRADE:<playerId>:<attribute>");
                    return;
                }
                String attribute = command[2];
                int cost = 5;

                if (player.getUpgradePoints() >= cost) {
                    player.setUpgradePoints(player.getUpgradePoints() - cost);
                    if ("BASE_ATTACK".equals(attribute)) {
                        player.setBaseAttack(player.getBaseAttack() + 1);
                    }
                    playerRepository.save(player);
                    logger.info("Jogador {} melhorou o atributo {} com sucesso", playerId, attribute);
                    out.println("SUCCESS:Melhoria aplicada! Novo ataque: " + player.getBaseAttack());
                } else {
                    logger.warn("Jogador {} tentou melhorar atributo {} mas não tem pontos suficientes", playerId, attribute);
                    out.println("ERROR:Pontos de melhoria insuficientes.");
                }
                break;

            default:
                logger.warn("Comando desconhecido recebido: {}", action);
                out.println("ERROR:Comando desconhecido.");
                break;
        }
    }

    private Player getOrCreatePlayer(String playerId) {
        Optional<Player> playerOpt = playerRepository.findById(playerId);
        return playerOpt.orElseGet(() -> {
            Player newPlayer = new Player(playerId, "Jogador " + playerId);
            playerRepository.save(newPlayer);
            logger.info("Novo jogador criado: {}", playerId);
            return newPlayer;
        });
    }
}