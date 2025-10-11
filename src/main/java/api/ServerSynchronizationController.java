package api;

import api.registry.ServerRegistry;
import controller.GameFacade;
import model.Player;
import model.TradeProposal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import repository.PlayerRepository;
import service.election.LeaderElectionService;
import service.lock.LockService;
import service.matchmaking.MatchmakingService;
import service.trade.TradeService;

import java.util.Set;

@Profile("server")
@RestController
@RequestMapping("/api")
public class ServerSynchronizationController {

    private final GameFacade gameFacade;
    private final ServerRegistry serverRegistry;
    private final PlayerRepository localPlayerRepository;
    private final MatchmakingService matchmakingService;
    private final LeaderElectionService leaderElectionService;
    private final LockService lockService;
    private final TradeService tradeService;

    @Autowired
    public ServerSynchronizationController(GameFacade gameFacade,
                                           ServerRegistry serverRegistry,
                                           @Qualifier("playerRepositoryJson") PlayerRepository localPlayerRepository,
                                           MatchmakingService matchmakingService,
                                           LeaderElectionService leaderElectionService,
                                           LockService lockService,
                                           TradeService tradeService) {
        this.gameFacade = gameFacade;
        this.serverRegistry = serverRegistry;
        this.localPlayerRepository = localPlayerRepository;
        this.matchmakingService = matchmakingService;
        this.leaderElectionService = leaderElectionService;
        this.lockService = lockService;
        this.tradeService = tradeService;
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

    @PostMapping("/lock/acquire")
    public ResponseEntity<String> acquireLock() {
        if (!leaderElectionService.isLeader()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("This server is not the leader.");
        }
        if (lockService.acquire()) {
            return ResponseEntity.ok("Lock acquired.");
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Lock already held.");
        }
    }

    @PostMapping("/lock/release")
    public ResponseEntity<String> releaseLock() {
        if (!leaderElectionService.isLeader()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("This server is not the leader.");
        }
        lockService.release();
        return ResponseEntity.ok("Lock released.");
    }

    @PostMapping("/trades/propose")
    public ResponseEntity<TradeProposal> proposeTrade(@RequestBody TradeProposal proposal) {
        tradeService.createTrade(proposal);
        
        String targetPlayerId = proposal.getTargetPlayerId();
        String notification = "UPDATE:TRADE_PROPOSAL:" + proposal.getTradeId() + ":" + proposal.getProposingPlayerId();
        gameFacade.notifyPlayer(targetPlayerId, notification);
        
        return ResponseEntity.ok(proposal);
    }

    @PostMapping("/trades/{tradeId}/accept")
    public ResponseEntity<String> acceptTrade(@PathVariable String tradeId) {
        boolean success = gameFacade.executeTrade(tradeId);
        if (success) {
            return ResponseEntity.ok("Trade accepted and executed.");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Trade failed.");
        }
    }
}