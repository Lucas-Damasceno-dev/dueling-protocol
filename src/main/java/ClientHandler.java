import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Optional;

import controller.GameFacade;
import model.Player;
import repository.PlayerRepository;
import repository.PlayerRepositoryJson;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final GameFacade gameFacade;
    private final PlayerRepository playerRepository;

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
                System.out.println("Comando recebido: " + inputLine);
                String[] command = inputLine.split(":");
                processCommand(command, out);
            }
        } catch (Exception e) {
            System.out.println("Cliente desconectado: " + e.getMessage());
        }
    }

    private void processCommand(String[] command, PrintWriter out) {
        String action = command[0];
        String playerId = command.length > 1 ? command[1] : null;

        if (playerId == null || playerId.trim().isEmpty()) {
            out.println("ERROR:ID do jogador não fornecido.");
            return;
        }

        Player player = getOrCreatePlayer(playerId);

        switch (action) {
            case "CHARACTER_SETUP":
                String race = command[2];
                String pClass = command[3];
                player.setCharacter(race, pClass);
                playerRepository.save(player);
                out.println("SUCCESS:Personagem configurado como " + race + " " + pClass);
                break;

            case "MATCHMAKING":
                gameFacade.enterMatchmaking(player);
                out.println("SUCCESS:Você entrou na fila.");
                gameFacade.tryToCreateMatch(out);
                break;

            case "STORE":
                String packType = command[3];
                gameFacade.buyPack(player, packType);
                playerRepository.save(player);
                out.println("SUCCESS:Pacote comprado.");
                break;

            case "GAME":
                gameFacade.processGameCommand(command, out);
                break;
            
            case "UPGRADE":
                String attribute = command[2];
                int cost = 5;

                if (player.getUpgradePoints() >= cost) {
                    player.setUpgradePoints(player.getUpgradePoints() - cost);
                    if ("BASE_ATTACK".equals(attribute)) {
                        player.setBaseAttack(player.getBaseAttack() + 1);
                    }
                    playerRepository.save(player);
                    out.println("SUCCESS:Melhoria aplicada! Novo ataque: " + player.getBaseAttack());
                } else {
                    out.println("ERROR:Pontos de melhoria insuficientes.");
                }
                break;

            default:
                out.println("ERROR:Comando desconhecido.");
                break;
        }
    }

    private Player getOrCreatePlayer(String playerId) {
        Optional<Player> playerOpt = playerRepository.findById(playerId);
        return playerOpt.orElseGet(() -> {
            Player newPlayer = new Player(playerId, "Jogador " + playerId);
            playerRepository.save(newPlayer);
            return newPlayer;
        });
    }
}