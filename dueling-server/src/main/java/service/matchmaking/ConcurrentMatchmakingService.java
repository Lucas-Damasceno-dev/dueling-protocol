package service.matchmaking;

import model.Player;
import model.Match;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Thread-safe implementation of the MatchmakingService interface.
 * Uses a concurrent queue to manage the player matchmaking queue and ensures
 * thread safety when multiple players are added to the queue simultaneously.
 * This service is managed by the Spring container as a singleton.
 */
@Service
public class ConcurrentMatchmakingService implements MatchmakingService {

    private final Queue<PlayerWithDeck> matchmakingQueue = new ConcurrentLinkedQueue<>();
    private final Object lock = new Object();
    
    private static final Logger logger = LoggerFactory.getLogger(ConcurrentMatchmakingService.class);

    /**
     * Public constructor for Spring's dependency injection.
     */
    public ConcurrentMatchmakingService() {}

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
        
        // The contains check is not atomic with the offer, but it prevents spamming the logs
        // for a player that is already waiting. ConcurrentLinkedQueue handles duplicates gracefully.
        PlayerWithDeck playerWithDeck = new PlayerWithDeck(player, null);
        if (!matchmakingQueue.contains(playerWithDeck)) {
            matchmakingQueue.offer(playerWithDeck);
            logger.info("{} entered the matchmaking queue", player.getNickname());
        } else {
            logger.debug("{} is already in the matchmaking queue", player.getNickname());
        }
    }
    
    /**
     * Adds a player to the matchmaking queue with their selected deck.
     *
     * @param player the player to add to the queue
     * @param deckId the ID of the deck the player wants to use
     */
    @Override
    public void addPlayerToQueueWithDeck(Player player, String deckId) {
        if (player == null) {
            logger.warn("Attempt to add null player to matchmaking queue with deck");
            return;
        }
        
        PlayerWithDeck playerWithDeck = new PlayerWithDeck(player, deckId);
        if (!matchmakingQueue.contains(playerWithDeck)) {
            matchmakingQueue.offer(playerWithDeck);
            logger.info("{} entered the matchmaking queue with deck {}", player.getNickname(), deckId);
        } else {
            logger.debug("{} is already in the matchmaking queue with deck {}", player.getNickname(), deckId);
        }
    }

    /**
     * Attempts to find a match between players in the queue.
     * This method is synchronized to ensure that match creation is atomic,
     * preventing a race condition where multiple threads might try to create a match with the same players.
     *
     * @return an Optional containing a Match if two players are available, or empty if not enough players
     */
    @Override
    public Optional<Match> findMatch() {
        synchronized (lock) {
            if (matchmakingQueue.size() >= 2) {
                PlayerWithDeck playerWithDeck1 = matchmakingQueue.poll();
                PlayerWithDeck playerWithDeck2 = matchmakingQueue.poll();

                if (playerWithDeck1 != null && playerWithDeck2 != null) {
                    logger.info("Match found: {} vs {}", 
                                playerWithDeck1.getPlayer().getNickname(), 
                                playerWithDeck2.getPlayer().getNickname());
                    Match match = new Match(playerWithDeck1.getPlayer(), playerWithDeck2.getPlayer());
                    return Optional.of(match);
                } else {
                    // This case can happen if a player disconnects right after being polled
                    logger.warn("Error forming match: one or both players are null after polling from queue.");
                    // Re-queue the non-null player if one exists
                    if (playerWithDeck1 != null) {
                        matchmakingQueue.offer(playerWithDeck1);
                    }
                    if (playerWithDeck2 != null) {
                        matchmakingQueue.offer(playerWithDeck2);
                    }
                }
            }
            return Optional.empty();
        }
    }

    @Override
    public Optional<Player> findAndLockPartner() {
        synchronized (lock) {
            if (!matchmakingQueue.isEmpty()) {
                PlayerWithDeck partnerWithDeck = matchmakingQueue.poll();
                if (partnerWithDeck != null) {
                    logger.info("Found and locked partner {} for remote match with deck {}.", 
                                partnerWithDeck.getPlayer().getNickname(), 
                                partnerWithDeck.getDeckId());
                    return Optional.ofNullable(partnerWithDeck.getPlayer());
                }
            }
            return Optional.empty();
        }
    }
}