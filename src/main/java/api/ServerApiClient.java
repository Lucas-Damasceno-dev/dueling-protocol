package api;

import model.Player;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;

import java.util.Set;

@Component
public class ServerApiClient {

    private final RestTemplate restTemplate;

    @Autowired
    public ServerApiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void registerWithServer(String targetServerUrl, String selfUrl) {
        // The URL for the endpoint is http://<targetServerUrl>/api/servers/register
        String registrationUrl = targetServerUrl + "/api/servers/register";
        restTemplate.postForEntity(registrationUrl, selfUrl, String.class);
    }

    public Set<String> getRegisteredServers(String serverUrl) {
        String url = serverUrl + "/api/servers";
        ResponseEntity<Set> response = restTemplate.getForEntity(url, Set.class);
        return response.getBody();
    }

    public void enqueuePlayer(String serverUrl, Player player) {
        String url = serverUrl + "/api/matchmaking/enqueue";
        restTemplate.postForEntity(url, player, String.class);
    }

    public Player getPlayer(String serverUrl, String playerId) {
        String url = serverUrl + "/api/players/" + playerId;
        ResponseEntity<Player> response = restTemplate.getForEntity(url, Player.class);
        return response.getBody();
    }
}
