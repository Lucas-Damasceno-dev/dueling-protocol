package client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

public class StockStressClient implements WebSocket.Listener {
    private final CompletableFuture<Void> closed = new CompletableFuture<>();
    private final String username;
    private final String password;
    private WebSocket webSocket;
    private String jwtToken;

    public StockStressClient(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public void connectAndBuy() throws Exception {
        // First, authenticate via HTTP to get JWT token
        authenticate();
        
        if (jwtToken == null || jwtToken.isEmpty()) {
            System.err.println("[" + username + "] Failed to obtain JWT token. Cannot connect to WebSocket.");
            return;
        }
        
        // Now connect to WebSocket with the JWT token
        String wsUrl = "ws://localhost:8080/ws?token=" + URLEncoder.encode(jwtToken, StandardCharsets.UTF_8.toString());
        
        HttpClient.newHttpClient().newWebSocketBuilder()
                .buildAsync(URI.create(wsUrl), this)
                .thenAccept(ws -> this.webSocket = ws)
                .join();
        
        // Wait until the WebSocket is closed by the server or an action, with timeout of 30 seconds
        try {
            closed.orTimeout(30, TimeUnit.SECONDS).join();
        } catch (java.util.concurrent.CompletionException e) {
            if (e.getCause() instanceof java.util.concurrent.TimeoutException) {
                System.out.println("[" + username + "] Operation timed out, closing connection...");
                if (webSocket != null) {
                    try {
                        webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Operation timed out");
                    } catch (Exception e2) {
                        System.err.println("[" + username + "] Error closing WebSocket: " + e2.getMessage());
                    }
                }
                // Complete the future if it's not already completed
                if (!closed.isDone()) {
                    closed.complete(null);
                }
            } else {
                throw e; // Re-throw if it's not a timeout exception
            }
        }
    }

    private void authenticate() throws Exception {
        String loginUrl = "http://localhost:8080/api/auth/login";
        
        // Prepare login data
        JsonObject credentials = new JsonObject();
        credentials.addProperty("username", username);
        credentials.addProperty("password", password);
        
        // Create HTTP request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(loginUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(credentials.toString()))
                .build();
        
        // Send request and get response
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200) {
            JsonObject responseJson = JsonParser.parseString(response.body()).getAsJsonObject();
            this.jwtToken = responseJson.get("token").getAsString();
            System.out.println("[" + username + "] Successfully obtained JWT token.");
        } else {
            // Try to register first if login fails
            System.out.println("[" + username + "] Login failed, attempting to register...");
            registerAndLogin();
        }
    }

    private void registerAndLogin() throws Exception {
        // First register
        String registerUrl = "http://localhost:8080/api/auth/register";
        
        JsonObject credentials = new JsonObject();
        credentials.addProperty("username", username);
        credentials.addProperty("password", password);
        
        HttpRequest registerRequest = HttpRequest.newBuilder()
                .uri(URI.create(registerUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(credentials.toString()))
                .build();
        
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> registerResponse = client.send(registerRequest, HttpResponse.BodyHandlers.ofString());
        
        if (registerResponse.statusCode() == 200) {
            System.out.println("[" + username + "] Successfully registered, now attempting to login...");
            // Then login
            authenticate(); // Call login again after registration
        } else {
            System.err.println("[" + username + "] Registration failed with status: " + registerResponse.statusCode() + ", body: " + registerResponse.body());
        }
    }

    @Override
    public void onOpen(WebSocket webSocketParam) {
        System.out.println("[" + username + "] WebSocket connection opened.");
        this.webSocket = webSocketParam;  // Atribui o WebSocket ao campo
        webSocketParam.request(1);
        
        // First, set up the character profile
        String characterSetupCommand = "CHARACTER_SETUP:" + username + ":HUMAN:WARRIOR";
        System.out.println("[" + username + "] Sending: " + characterSetupCommand);
        webSocketParam.sendText(characterSetupCommand, true);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocketParam, CharSequence data, boolean last) {
        String message = data.toString();
        System.out.println("[" + username + "] Received: " + message);
        webSocketParam.request(1);

        if (message.contains("SUCCESS:Character created.")) {
            // Character setup successful, now send the buy command
            String buyCommand = "STORE:BUY:LEGENDARY";
            System.out.println("[" + username + "] Sending: " + buyCommand);
            webSocketParam.sendText(buyCommand, true);
        } else if (message.contains("SUCCESS:Pack purchased") || message.contains("OUT_OF_STOCK") || message.contains("ERROR")) {
            // Close the connection after receiving the result
            webSocketParam.sendClose(WebSocket.NORMAL_CLOSURE, "Operation completed.");
        }
        
        return null;
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocketParam, int statusCode, String reason) {
        System.out.println("[" + username + "] WebSocket connection closed. Reason: " + reason);
        closed.complete(null);
        return null;
    }

    @Override
    public void onError(WebSocket webSocketParam, Throwable error) {
        System.err.println("[" + username + "] WebSocket error: " + error.getMessage());
        error.printStackTrace();
        closed.completeExceptionally(error);
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java StockStressClient <username> <password>");
            return;
        }
        try {
            StockStressClient client = new StockStressClient(args[0], args[1]);
            client.connectAndBuy();
        } catch (Exception e) {
            System.err.println("Stress client failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
