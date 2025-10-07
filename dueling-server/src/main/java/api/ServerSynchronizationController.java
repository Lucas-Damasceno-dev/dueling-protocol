package api;

import api.registry.ServerRegistry;
import controller.GameFacade;
import model.Player;
import model.TradeProposal;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    private static final Logger logger = LoggerFactory.getLogger(ServerSynchronizationController.class);

    private final GameFacade gameFacade;
    private final ServerRegistry serverRegistry;
    private final PlayerRepository localPlayerRepository;
    private final MatchmakingService matchmakingService;
    private final LeaderElectionService leaderElectionService;
    private final LockService lockService;
    private final TradeService tradeService;

    @Autowired
    public ServerSynchronizationController(GameFacade aGameFacade,
                                           ServerRegistry aServerRegistry,
                                           PlayerRepository aLocalPlayerRepository,
                                           MatchmakingService aMatchmakingService,
                                           LeaderElectionService aLeaderElectionService,
                                           LockService aLockService,
                                           TradeService aTradeService) {
        this.gameFacade = aGameFacade;
        this.serverRegistry = aServerRegistry;
        this.localPlayerRepository = aLocalPlayerRepository;
        this.matchmakingService = aMatchmakingService;
        this.leaderElectionService = aLeaderElectionService;
        this.lockService = aLockService;
        this.tradeService = aTradeService;
    }

    /**
     * Registers a new server instance with the server registry.
     * This endpoint is used by other server instances to announce their presence
     * and become part of the distributed system.
     *
     * @param serverUrl The URL of the server to register.
     * @return A {@link ResponseEntity} indicating the success or failure of the registration.
     */
    @PostMapping("/servers/register")
    public ResponseEntity<String> registerServer(@RequestBody String serverUrl) {
        serverRegistry.registerServer(serverUrl);
        return ResponseEntity.ok("Server registered successfully: " + serverUrl);
    }

    /**
     * Retrieves a list of all currently registered server URLs.
     *
     * @return A {@link ResponseEntity} containing a {@link Set} of server URLs.
     */
    @GetMapping("/servers")
    public ResponseEntity<Set<String>> getRegisteredServers() {
        return ResponseEntity.ok(serverRegistry.getRegisteredServers());
    }

    /**
     * Enqueues a player into the matchmaking system.
     * This endpoint is typically called by other servers to add players from their local queues
     * to the global matchmaking pool.
     *
     * @param player The {@link Player} object to enqueue.
     * @return A {@link ResponseEntity} indicating the success of enqueuing the player.
     */
    @PostMapping("/matchmaking/enqueue")
    public ResponseEntity<String> enqueuePlayer(@RequestBody Player player) {
        matchmakingService.addPlayerToQueue(player);
        gameFacade.tryToCreateMatch(); 
        return ResponseEntity.ok("Player " + player.getId() + " added to matchmaking queue.");
    }

    /**
     * Retrieves a player's information by their ID.
     *
     * @param id The unique identifier of the player.
     * @return A {@link ResponseEntity} containing the {@link Player} object if found,
     *         or a 404 Not Found status if the player does not exist.
     */
    @GetMapping("/players/{id}")
    public ResponseEntity<Player> getPlayer(@PathVariable String id) {
        Player player = gameFacade.findPlayerById(id);
        if (player != null) {
            return ResponseEntity.ok(player);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Endpoint for committing a distributed transaction.
     * This method is intended to be called by the leader server to finalize a transaction
     * across multiple server instances.
     *
     * @param transactionDetails A string containing the details of the transaction to commit.
     * @return A {@link ResponseEntity} indicating the transaction was processed.
     */
    @PostMapping("/transactions/commit")
    public ResponseEntity<String> commitTransaction(@RequestBody String transactionDetails) {
        logger.info("Received commit request for transaction: {}", transactionDetails);
        return ResponseEntity.ok("Commit transaction processed.");
    }

    /**
     * Saves a player's data to the local repository.
     * This is typically used for synchronization between server instances,
     * where one server might send player data to another for persistence.
     *
     * @param player The {@link Player} object to save.
     * @return A {@link ResponseEntity} indicating the success of saving the player.
     */
    @PostMapping("/players")
    public ResponseEntity<String> savePlayer(@RequestBody Player player) {
        localPlayerRepository.save(player);
        return ResponseEntity.ok("Player " + player.getId() + " saved locally.");
    }

    /**
     * Attempts to find and lock a player from the matchmaking queue to serve as a partner.
     * This endpoint is called by other servers when they need a remote player
     * to complete a match.
     *
     * @return A {@link ResponseEntity} containing the locked {@link Player} if successful,
     *         or a 404 Not Found status if no partner could be found.
     */
    @PostMapping("/matchmaking/find-and-lock-partner")
    public ResponseEntity<Player> findAndLockPartner() {
        return matchmakingService.findAndLockPartner()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Acquires a distributed lock.
     * This endpoint can only be called by the current leader server.
     * It attempts to acquire a global lock to ensure exclusive access to a critical section.
     *
     * @return A {@link ResponseEntity} indicating whether the lock was successfully acquired
     *         or if it was already held, or if the calling server is not the leader.
     */
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

    /**
     * Releases a previously acquired distributed lock.
     * This endpoint can only be called by the current leader server.
     *
     * @return A {@link ResponseEntity} indicating that the lock has been released,
     *         or if the calling server is not the leader.
     */
    @PostMapping("/lock/release")
    public ResponseEntity<String> releaseLock() {
        if (!leaderElectionService.isLeader()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("This server is not the leader.");
        }
        lockService.release();
        return ResponseEntity.ok("Lock released.");
    }

    /**
     * Receives a trade proposal from another server and registers it.
     * This endpoint is used in a distributed environment to propagate trade proposals
     * across different server instances.
     *
     * @param proposal The {@link TradeProposal} object containing details of the trade.
     * @return A {@link ResponseEntity} containing the created {@link TradeProposal}.
     */
    @PostMapping("/trades/propose")
    public ResponseEntity<TradeProposal> proposeTrade(@RequestBody TradeProposal proposal) {
        tradeService.createTrade(proposal);
        
        String targetPlayerId = proposal.getTargetPlayerId();
        String notification = "UPDATE:TRADE_PROPOSAL:" + proposal.getTradeId() + ":" + proposal.getProposingPlayerId();
        gameFacade.notifyPlayer(targetPlayerId, notification);
        
        return ResponseEntity.ok(proposal);
    }

    /**
     * Accepts and executes a trade proposal.
     * This endpoint is called to finalize a trade that has been proposed and accepted.
     *
     * @param tradeId The unique identifier of the trade to accept.
     * @return A {@link ResponseEntity} indicating the success or failure of the trade execution.
     */
    @PostMapping("/trades/{tradeId}/accept")
    public ResponseEntity<String> acceptTrade(@PathVariable String tradeId) {
        boolean success = gameFacade.executeTrade(tradeId);
        if (success) {
            return ResponseEntity.ok("Trade accepted and executed.");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Trade failed.");
        }
    }
    
    /**
     * Provides a basic health check endpoint for the server.
     *
     * @return A {@link ResponseEntity} with a success message if the server is running.
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Server is healthy");
    }
}
