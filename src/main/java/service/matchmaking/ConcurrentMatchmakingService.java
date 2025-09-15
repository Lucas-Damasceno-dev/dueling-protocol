package service.matchmaking;

import model.Player;
import model.Match;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread-safe implementation of the MatchmakingService interface.
 * Uses a concurrent queue to manage the player matchmaking queue and ensures
 * thread safety when multiple players are added to the queue simultaneously.
 */
public class ConcurrentMatchmakingService implements MatchmakingService {

    private static volatile ConcurrentMatchmakingService instance;
    private final Queue<Player> matchmakingQueue = new ConcurrentLinkedQueue<>();
    private final Object lock = new Object();
    
    private static final Logger logger = LoggerFactory.getLogger(ConcurrentMatchmakingService.class);

    /**
     * Private constructor to enforce singleton pattern.
     */
    private ConcurrentMatchmakingService() {}

    /**
     * Gets the singleton instance of ConcurrentMatchmakingService.
     * Uses double-checked locking for thread-safe lazy initialization.
     *
     * @return the singleton instance of ConcurrentMatchmakingService
     */
    public static ConcurrentMatchmakingService getInstance() {
        if (instance == null) {
            synchronized (ConcurrentMatchmakingService.class) {
                if (instance == null) {
                    instance = new ConcurrentMatchmakingService();
                }
            }
        }
        return instance;
    }

    /**
     * {@inheritDoc}
     * Adds a player to the matchmaking queue if they are not already in it.
     *
     * @param player the player to add to the queue
     */
    @Override
    public void addPlayerToQueue(Player player) {
        if (player == null) {
            logger.warn("Attempt to add null player to matchmaking queue");
            return;
        }
        
        if (!matchmakingQueue.contains(player)) {
            matchmakingQueue.offer(player);
            logger.info("{} entered the matchmaking queue", player.getNickname());
        } else {
            logger.debug("{} is already in the matchmaking queue", player.getNickname());
        }
    }

    /**
     * {@inheritDoc}
     * Attempts to find a match between players in the queue.
     * This method is synchronized to ensure that match creation is atomic.
     *
     * @return an Optional containing a Match if two players are available, or empty if not enough players
     */
    @Override
    public Optional<Match> findMatch() {
        synchronized (lock) {
            if (matchmakingQueue.size() >= 2) {
                Player player1 = matchmakingQueue.poll();
                Player player2 = matchmakingQueue.poll();

                if (player1 != null && player2 != null) {
                    logger.info("Match found: {} vs {}", player1.getNickname(), player2.getNickname());
                    Match match = new Match(player1, player2);
                    return Optional.of(match);
                } else {
                    logger.warn("Error forming match: one or both players are null");
                }
            }
            return Optional.empty();
        }
    }
}