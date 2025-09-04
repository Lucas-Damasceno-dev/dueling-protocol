import java.io.*;
import java.net.*;

public class GameClient {
    private static final String SERVER_ADDRESS = System.getenv().getOrDefault("SERVER_HOST", "127.0.0.1");
    private static final int TCP_PORT = 7777;
    private static final int UDP_PORT = 7778;
    
    public static void main(String[] args) {
        try {
            Socket socket = new Socket(SERVER_ADDRESS, TCP_PORT);
            System.out.println("Conectado ao servidor!");
            
            Thread receiverThread = new Thread(new ServerMessageReceiver(socket));
            receiverThread.start();
            
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            String playerId = "player123";

            out.println("MATCHMAKING:" + playerId + ":ENTER");
            
            out.println("STORE:" + playerId + ":BUY:BASIC");
            
            checkPing();
            
        } catch (Exception e) {
            System.err.println("Erro no cliente: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static void checkPing() {
        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress address = InetAddress.getByName(SERVER_ADDRESS);
            
            long startTime = System.currentTimeMillis();
            String message = String.valueOf(startTime);
            byte[] buffer = message.getBytes();

            DatagramPacket request = new DatagramPacket(buffer, buffer.length, address, UDP_PORT);
            socket.send(request);
            
            DatagramPacket response = new DatagramPacket(new byte[buffer.length], buffer.length);
            socket.setSoTimeout(1000);
            socket.receive(response);

            long endTime = System.currentTimeMillis();
            long latency = endTime - startTime;
            System.out.println("Ping: " + latency + " ms");

        } catch (IOException e) {
            System.err.println("Ping falhou: " + e.getMessage());
        }
    }
    
    static class ServerMessageReceiver implements Runnable {
        private Socket socket;
        
        public ServerMessageReceiver(Socket socket) {
            this.socket = socket;
        }
        
        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                String response;
                while ((response = in.readLine()) != null) {
                    System.out.println("Servidor: " + response);
                }
            } catch (IOException e) {
                System.err.println("Erro ao receber mensagem do servidor: " + e.getMessage());
            }
        }
    }
}