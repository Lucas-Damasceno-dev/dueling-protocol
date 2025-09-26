import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * UDP server for ping measurements.
 * This server acts as an echo server, receiving UDP packets and immediately sending them back.
 * Clients can measure round-trip time to determine network latency.
 */
public class PingServer implements Runnable {
    private final int port;
    
    private static final Logger logger = LoggerFactory.getLogger(PingServer.class);

    /**
     * Constructs a new PingServer that will listen on the specified port.
     *
     * @param port the UDP port to listen on
     */
    public PingServer(int port) { 
        this.port = port; 
    }

    /**
     * Main method that runs in a separate thread to handle UDP ping requests.
     * Receives UDP packets and immediately sends them back to the sender.
     */
    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket(port)) {
            logger.info("UDP Ping Server listening on port {}", port);
            byte[] buffer = new byte[256];
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                socket.send(packet);
            }
        } catch (IOException e) {
            logger.error("Error in Ping Server: {}", e.getMessage(), e);
        }
    }
}