import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import controller.GameFacade;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main server class for the Dueling Protocol card game.
 * This class starts the TCP server for game communication and the UDP server for ping measurements.
 */
public class GameServer {
    public static final int TCP_PORT = 7777;
    public static final int UDP_PORT = 7778;
    
    private static final Logger logger = LoggerFactory.getLogger(GameServer.class);

    /**
     * Main method that starts the game server.
     * Initializes the game facade and starts listening for client connections on the TCP port.
     * Also starts the ping server on the UDP port.
     *
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {
        GameFacade gameFacade = new GameFacade();
        logger.info("Card Game Server started on port {}", TCP_PORT);

        try (ServerSocket serverSocket = new ServerSocket(TCP_PORT)) {
            PingServer pingServer = new PingServer(UDP_PORT);
            new Thread(pingServer).start();
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                logger.info("New client connected: {}", clientSocket.getInetAddress());

                ClientHandler clientHandler = new ClientHandler(clientSocket, gameFacade);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            logger.error("Server error: {}", e.getMessage(), e);
        }
    }
}