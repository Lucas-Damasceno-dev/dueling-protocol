package service.matchmaking;

import model.Player;
import model.Match;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import repository.MatchRepository;

/**
 * Thread-safe implementation of the MatchmakingService interface for local development.
 * This version does not use LeaderElectionService.
 */
@Service
@Profile("local-dev")
public class LocalDevMatchmakingService implements MatchmakingService {

    private static final Logger logger = LoggerFactory.getLogger(LocalDevMatchmakingService.class);
    private final Queue<PlayerWithDeck> matchmakingQueue = new ConcurrentLinkedQueue<>();
    private final Object lock = new Object();
    private final MatchRepository matchRepository;

    /**
     * Public constructor for Spring's dependency injection.
     */
    public LocalDevMatchmakingService(MatchRepository matchRepository) {
        this.matchRepository = matchRepository;
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
    private static final int MAX_ELO_DIFFERENCE = 100; // Define a suitable Elo difference

    @Override
    public Optional<Match> findMatch() {
        synchronized (lock) {
            if (matchmakingQueue.size() < 2) {
                return Optional.empty();
            }

            PlayerWithDeck playerWithDeck1 = matchmakingQueue.poll();
            if (playerWithDeck1 == null) {
                return Optional.empty();
            }

            Player player1 = playerWithDeck1.getPlayer();
            int player1Elo = player1.getPlayerRanking().getEloRating();

            // Iterate through the queue to find a suitable opponent
            for (PlayerWithDeck playerWithDeck2 : matchmakingQueue) {
                Player player2 = playerWithDeck2.getPlayer();
                int player2Elo = player2.getPlayerRanking().getEloRating();

                if (Math.abs(player1Elo - player2Elo) <= MAX_ELO_DIFFERENCE) {
                    // Found a suitable match, remove player2 from the queue
                    matchmakingQueue.remove(playerWithDeck2);
                    logger.info("Match found (Elo-based): {} ({}) vs {} ({})",
                                player1.getNickname(), player1Elo,
                                player2.getNickname(), player2Elo);
                    Match match = new Match(player1, player2);
                    match.setServerUrl("http://localhost:8083"); // Hardcoded for local dev
                    return Optional.of(match);
                }
            }

            // If no suitable match is found, re-add player1 to the queue
            matchmakingQueue.offer(playerWithDeck1);
            logger.debug("No suitable Elo-based match found for {}. Re-adding to queue.", player1.getNickname());
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
    
    @Override
    public boolean isPlayerInQueue(Player player) {
        if (player == null) {
            return false;
        }
        PlayerWithDeck target = new PlayerWithDeck(player, null);
        // Check for the player with null deck
        boolean isInQueue = matchmakingQueue.contains(target);
        if (isInQueue) {
            return true;
        }
        // Check for the player with any deck
        return matchmakingQueue.stream().anyMatch(p -> p.getPlayer() != null && p.getPlayer().getId().equals(player.getId()));
    }
}