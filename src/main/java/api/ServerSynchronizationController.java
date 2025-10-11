package api;

import api.registry.ServerRegistry;
import controller.GameFacade;
import model.Player;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api") // Changed from /api/sync
public class ServerSynchronizationController {

    private final GameFacade gameFacade;
    private final ServerRegistry serverRegistry;

    @Autowired
    public ServerSynchronizationController(GameFacade gameFacade, ServerRegistry serverRegistry) {
        this.gameFacade = gameFacade;
        this.serverRegistry = serverRegistry;
    }

    // Endpoint from the plan: POST /api/servers/register
    @PostMapping("/servers/register")
    public ResponseEntity<String> registerServer(@RequestBody String serverUrl) {
        serverRegistry.registerServer(serverUrl);
        // TODO: Add logic to handle server registration, e.g., health checks
        return ResponseEntity.ok("Server registered successfully: " + serverUrl);
    }

    // Endpoint from the plan: GET /api/servers
    @GetMapping("/servers")
    public ResponseEntity<Set<String>> getRegisteredServers() {
        return ResponseEntity.ok(serverRegistry.getRegisteredServers());
    }

    // Endpoint from the plan: POST /api/matchmaking/enqueue
    @PostMapping("/matchmaking/enqueue")
    public ResponseEntity<String> enqueuePlayer(@RequestBody Player player) {
        gameFacade.enterMatchmaking(player);
        return ResponseEntity.ok("Player " + player.getId() + " added to matchmaking queue.");
    }

    // Endpoint from the plan: GET /api/players/{id}
    @GetMapping("/players/{id}")
    public ResponseEntity<Player> getPlayer(@PathVariable String id) {
        // This assumes GameFacade has a method to find a player.
        // This needs to be implemented in a distributed way later.
        Player player = gameFacade.findPlayerById(id); // Assuming this method exists
        if (player != null) {
            return ResponseEntity.ok(player);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // Endpoint from the plan: POST /api/transactions/commit
    @PostMapping("/transactions/commit")
    public ResponseEntity<String> commitTransaction(@RequestBody String transactionDetails) {
        // Placeholder for the Two-Phase Commit (2PC) logic
        System.out.println("Received commit request for transaction: " + transactionDetails);
        // In a real implementation, this would trigger the commit phase of 2PC
        return ResponseEntity.ok("Commit transaction processed.");
    }

    @PostMapping("/players")
    public ResponseEntity<String> savePlayer(@RequestBody Player player) {
        localPlayerRepository.save(player);
        return ResponseEntity.ok("Player " + player.getId() + " saved locally.");
    }
}
