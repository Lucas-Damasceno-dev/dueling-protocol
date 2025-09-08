import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PingServer implements Runnable {
    private final int port;
    
    private static final Logger logger = LoggerFactory.getLogger(PingServer.class);

    public PingServer(int port) { 
        this.port = port; 
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket(port)) {
            logger.info("Servidor de Ping UDP ouvindo na porta {}", port);
            byte[] buffer = new byte[256];
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                socket.send(packet);
            }
        } catch (IOException e) {
            logger.error("Erro no servidor de Ping: {}", e.getMessage(), e);
        }
    }
}