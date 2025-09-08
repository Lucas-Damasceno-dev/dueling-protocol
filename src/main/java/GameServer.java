import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import controller.GameFacade;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GameServer {
    public static final int TCP_PORT = 7777;
    public static final int UDP_PORT = 7778;
    
    private static final Logger logger = LoggerFactory.getLogger(GameServer.class);

    public static void main(String[] args) {
        GameFacade gameFacade = new GameFacade();
        logger.info("Servidor do Jogo de Cartas iniciado na porta {}", TCP_PORT);

        try (ServerSocket serverSocket = new ServerSocket(TCP_PORT)) {
            PingServer pingServer = new PingServer(UDP_PORT);
            new Thread(pingServer).start();
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                logger.info("Novo cliente conectado: {}", clientSocket.getInetAddress());

                ClientHandler clientHandler = new ClientHandler(clientSocket, gameFacade);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            logger.error("Erro no servidor: {}", e.getMessage(), e);
        }
    }
}