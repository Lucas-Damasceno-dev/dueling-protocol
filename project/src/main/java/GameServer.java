import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import controller.GameFacade;

public class GameServer {
    public static final int TCP_PORT = 7777; // Porta para a comunicação do jogo
    public static final int UDP_PORT = 7778; // Porta para o ping

    public static void main(String[] args) {
        // Inicializa os serviços centrais (matchmaking, etc.)
        GameFacade gameFacade = new GameFacade();
        System.out.println("Servidor do Jogo de Cartas iniciado na porta " + TCP_PORT);

        try (ServerSocket serverSocket = new ServerSocket(TCP_PORT)) {
            // Inicia o servidor de ping em uma nova thread
            PingServer pingServer = new PingServer(UDP_PORT);
            new Thread(pingServer).start();
            
            while (true) {
                // Aguarda um novo cliente se conectar
                Socket clientSocket = serverSocket.accept();
                System.out.println("Novo cliente conectado: " + clientSocket.getInetAddress());

                // Cria uma nova thread para lidar com este cliente
                // Passa a fachada do jogo para que o handler possa executar as ações
                ClientHandler clientHandler = new ClientHandler(clientSocket, gameFacade);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            System.err.println("Erro no servidor: " + e.getMessage());
        }
    }
}