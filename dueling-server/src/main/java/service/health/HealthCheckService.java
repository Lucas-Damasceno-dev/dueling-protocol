package service.health;

import api.registry.ServerRegistry;
import model.Match;
import model.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Profile("server")
@Service
public class HealthCheckService {

    private static final Logger logger = LoggerFactory.getLogger(HealthCheckService.class);
    private static final int MAX_FAILED_CHECKS = 3;
    private static final String HEALTH_CHECK_ENDPOINT = "/api/health";
    
    private final ServerRegistry serverRegistry;
    private final RestTemplate restTemplate;
    private final String serverName;
    private final String serverPort;
    private final repository.MatchRepository matchRepository;
    private final websocket.WebSocketSessionManager webSocketSessionManager;
    
    // Track failed health checks count for each server
    private final Map<String, Integer> serverHealthCheckFailures = new HashMap<>();

    @Autowired
    public HealthCheckService(ServerRegistry serverRegistry, 
                             @Value("${server.name}") String serverName,
                             @Value("${server.port}") String serverPort,
                             repository.MatchRepository matchRepository,
                             websocket.WebSocketSessionManager webSocketSessionManager) {
        this.serverRegistry = serverRegistry;
        this.restTemplate = new RestTemplate();
        this.serverName = serverName;
        this.serverPort = serverPort;
        this.matchRepository = matchRepository;
        this.webSocketSessionManager = webSocketSessionManager;
    }

    private String getSelfUrl() {
        return "http://" + serverName + ":" + serverPort;
    }

    @Scheduled(fixedRate = 5000) // Check every 5 seconds
    public void performHealthChecks() {
        logger.debug("Performing health checks on registered servers...");
        
        String selfUrl = getSelfUrl();
        Set<String> registeredServers = serverRegistry.getRegisteredServers();
        
        // Create a copy to avoid concurrent modification
        Set<String> serversToCheck = Set.copyOf(registeredServers);
        
        for (String serverUrl : serversToCheck) {
            if (serverUrl.equals(selfUrl)) {
                continue; // Don't check ourselves
            }
            
            boolean isHealthy = checkServerHealth(serverUrl);
            
            if (!isHealthy) {
                // Server is not responding, increment failure counter
                int currentFailures = serverHealthCheckFailures.getOrDefault(serverUrl, 0);
                currentFailures++;
                serverHealthCheckFailures.put(serverUrl, currentFailures);
                
                logger.debug("Server {} failed health check (failure count: {})", serverUrl, currentFailures);
                
                if (currentFailures >= MAX_FAILED_CHECKS) {
                    logger.warn("Removing server {} from registry after {} failed health checks", 
                               serverUrl, MAX_FAILED_CHECKS);
                    serverRegistry.unregisterServer(serverUrl);
                    serverHealthCheckFailures.remove(serverUrl); // Remove from tracking after removal
                }
            } else {
                // Server is healthy, reset failure counter
                serverHealthCheckFailures.remove(serverUrl);
            }
        }
        
        logger.debug("Health checks completed. Servers in registry: {}", 
                    serverRegistry.getRegisteredServers().size());
    }

    private boolean checkServerHealth(String serverUrl) {
        try {
            String healthUrl = serverUrl + HEALTH_CHECK_ENDPOINT;
            String response = restTemplate.getForObject(healthUrl, String.class);
            return response != null && response.contains("healthy");
        } catch (RestClientException e) {
            logger.debug("Health check failed for server {}: {}", serverUrl, e.getMessage());
            return false;
        }
    }

    private void handleServerFailure(String serverUrl) {
        logger.info("Handling failure of server: {}", serverUrl);
        List<Match> matches = matchRepository.findByServerUrl(serverUrl);
        for (Match match : matches) {
            if (match.getStatus() == Match.Status.IN_PROGRESS) {
                logger.info("Match {} was in progress on failed server. Notifying opponent.", match.getId());

                Player player1 = match.getPlayer1();
                Player player2 = match.getPlayer2();

                // Since the match was on the failed server, determine the appropriate opponent
                // If the match server is the one that failed, the opponent would be the other player
                Player opponent = player2; // Default to player2

                // Notify the opponent
                java.io.PrintWriter opponentWriter = webSocketSessionManager.getPlayerWriter(opponent.getId());
                if (opponentWriter != null) {
                    opponentWriter.println("OPPONENT_DISCONNECTED");
                    opponentWriter.flush();
                }

                // Update the match status
                match.setStatus(Match.Status.FINISHED);
                match.setWinner(opponent);
                matchRepository.save(match);

                logger.info("Match {} finished due to server failure. Winner: {}", match.getId(), opponent.getNickname());
            }
        }
    }
}