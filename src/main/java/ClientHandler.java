import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import controller.GameFacade;
import model.Player;
import java.util.Optional;
import repository.InMemoryPlayerRepository;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final GameFacade gameFacade;
    private Player player;
    private static final InMemoryPlayerRepository playerRepository = new InMemoryPlayerRepository();

    public ClientHandler(Socket socket, GameFacade facade) {
        this.clientSocket = socket;
        this.gameFacade = facade;
    }

    @Override
    public void run() {
        try (
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))
        ) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                String[] command = inputLine.split(":");
                processCommand(command, out);
            }
        } catch (Exception e) {
            System.out.println("Cliente desconectado: " + e.getMessage());
        }
    }

    private void processCommand(String[] command, PrintWriter out) {
        String action = command[0];
        if ("MATCHMAKING".equals(action)) {
             Player player = getPlayerById(command[2]);
             gameFacade.enterMatchmaking(player);
            out.println("SUCCESS:Você entrou na fila.");
            gameFacade.tryToCreateMatch(out);
        } else if ("STORE".equals(action)) {
             Player player = getPlayerById(command[2]);
             gameFacade.buyPack(player, command[3]);
            out.println("SUCCESS:Pacote comprado.");
        } else if ("GAME".equals(action)) {
            gameFacade.processGameCommand(command, out);
        } else {
            out.println("ERROR:Comando desconhecido.");
        }
    }
    
    private Player getPlayerById(String playerId) {
        return new Player(playerId, "Jogador " + playerId);
    }
}