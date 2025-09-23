import java.io.*;
import java.net.*;

public class GameClient {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int TCP_PORT = 7777;
    private static final int UDP_PORT = 7778;
    
    public static void main(String[] args) {
        try {
            // Conecta ao servidor TCP
            Socket socket = new Socket(SERVER_ADDRESS, TCP_PORT);
            System.out.println("Conectado ao servidor!");
            
            // Inicia uma thread para receber mensagens do servidor
            Thread receiverThread = new Thread(new ServerMessageReceiver(socket));
            receiverThread.start();
            
            // Exemplo de como enviar comandos ao servidor
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            
            // Exemplo de entrar na fila de matchmaking
            out.println("MATCHMAKING:ENTER:player123");
            
            // Exemplo de comprar um pacote
            out.println("STORE:BUY:player123:BASIC_PACK");
            
            // Exemplo de verificar ping
            checkPing();
            
        } catch (Exception e) {
            System.err.println("Erro no cliente: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static void checkPing() {
        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress address = InetAddress.getByName(SERVER_ADDRESS);
            
            // Envia o timestamp atual
            long startTime = System.currentTimeMillis();
            String message = String.valueOf(startTime);
            byte[] buffer = message.getBytes();

            DatagramPacket request = new DatagramPacket(buffer, buffer.length, address, UDP_PORT);
            socket.send(request);
            
            // Aguarda a resposta
            DatagramPacket response = new DatagramPacket(new byte[buffer.length], buffer.length);
            socket.setSoTimeout(1000); // Timeout de 1 segundo
            socket.receive(response);

            // Calcula a latência
            long endTime = System.currentTimeMillis();
            long latency = endTime - startTime;
            System.out.println("Ping: " + latency + " ms");

        } catch (IOException e) {
            System.err.println("Ping falhou: " + e.getMessage());
        }
    }
    
    // Classe interna para receber mensagens do servidor
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