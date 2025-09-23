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
    private Player player; // Player associado a este handler
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
                // Exemplo de um protocolo simples baseado em texto
                // Ex: "MATCHMAKING:ENTER:playerId123"
                // Ex: "STORE:BUY:playerId123:BASIC_PACK"
                String[] command = inputLine.split(":");
                processCommand(command, out);
            }
        } catch (Exception e) {
            System.out.println("Cliente desconectado: " + e.getMessage());
        }
    }

    private void processCommand(String[] command, PrintWriter out) {
        String action = command[0];
        // ... Lógica para extrair Player, etc.

        if ("MATCHMAKING".equals(action)) {
            // Player player = getPlayerById(command[2]);
            // gameFacade.enterMatchmaking(player);
            out.println("SUCCESS:Você entrou na fila.");
        } else if ("STORE".equals(action)) {
            // Player player = getPlayerById(command[2]);
            // gameFacade.buyPack(player, command[3]);
            out.println("SUCCESS:Compra realizada.");
        } else {
            out.println("ERROR:Comando desconhecido.");
        }
    }
    
    private Player getPlayerById(String playerId) {
        // Em uma implementação real, você buscaria o jogador no repositório
        // Por enquanto, vamos criar um jogador temporário
        return new Player(playerId, "Jogador " + playerId);
    }
}