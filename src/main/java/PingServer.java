import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class PingServer implements Runnable {
    private final int port;

    public PingServer(int port) { 
        this.port = port; 
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket(port)) {
            System.out.println("Servidor de Ping UDP ouvindo na porta " + port);
            byte[] buffer = new byte[256];
            while (true) {
                // Aguarda um pacote de ping
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                // Apenas ecoa o pacote de volta para o remetente
                socket.send(packet);
            }
        } catch (IOException e) {
            System.err.println("Erro no servidor de Ping: " + e.getMessage());
        }
    }
}