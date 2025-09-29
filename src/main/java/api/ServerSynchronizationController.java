package api;

import api.registry.ServerRegistry;
import controller.GameFacade;
import model.Player;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import repository.PlayerRepository;
import service.matchmaking.MatchmakingService;

import java.util.Set;

@RestController
@RequestMapping("/api")
public class ServerSynchronizationController {

    private final GameFacade gameFacade;
    private final ServerRegistry serverRegistry;
    private final PlayerRepository localPlayerRepository;
    private final MatchmakingService matchmakingService;

    @Autowired
    public ServerSynchronizationController(GameFacade gameFacade,
                                           ServerRegistry serverRegistry,
                                           @Qualifier("playerRepositoryJson") PlayerRepository localPlayerRepository,
                                           MatchmakingService matchmakingService) {
        this.gameFacade = gameFacade;
        this.serverRegistry = serverRegistry;
        this.localPlayerRepository = localPlayerRepository;
        this.matchmakingService = matchmakingService;
    }

    @PostMapping("/servers/register")
    public ResponseEntity<String> registerServer(@RequestBody String serverUrl) {
        serverRegistry.registerServer(serverUrl);
        return ResponseEntity.ok("Server registered successfully: " + serverUrl);
    }

    @GetMapping("/servers")
    public ResponseEntity<Set<String>> getRegisteredServers() {
        return ResponseEntity.ok(serverRegistry.getRegisteredServers());
    }

    @PostMapping("/matchmaking/enqueue")
    public ResponseEntity<String> enqueuePlayer(@RequestBody Player player) {
        matchmakingService.addPlayerToQueue(player);
        gameFacade.tryToCreateMatch(); 
        return ResponseEntity.ok("Player " + player.getId() + " added to matchmaking queue.");
    }

    @GetMapping("/players/{id}")
    public ResponseEntity<Player> getPlayer(@PathVariable String id) {
        Player player = gameFacade.findPlayerById(id);
        if (player != null) {
            return ResponseEntity.ok(player);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/transactions/commit")
    public ResponseEntity<String> commitTransaction(@RequestBody String transactionDetails) {
        System.out.println("Received commit request for transaction: " + transactionDetails);
        return ResponseEntity.ok("Commit transaction processed.");
    }

    @PostMapping("/players")
    public ResponseEntity<String> savePlayer(@RequestBody Player player) {
        localPlayerRepository.save(player);
        return ResponseEntity.ok("Player " + player.getId() + " saved locally.");
    }

    @PostMapping("/matchmaking/find-and-lock-partner")
    public ResponseEntity<Player> findAndLockPartner() {
        return matchmakingService.findAndLockPartner()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}