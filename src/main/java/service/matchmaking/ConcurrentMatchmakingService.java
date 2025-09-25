package service.matchmaking;

import model.Player;
import model.Match;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConcurrentMatchmakingService implements MatchmakingService {

    private static volatile ConcurrentMatchmakingService instance;
    private final Queue<Player> matchmakingQueue = new ConcurrentLinkedQueue<>();
    private final Object lock = new Object();
    
    private static final Logger logger = LoggerFactory.getLogger(ConcurrentMatchmakingService.class);

    private ConcurrentMatchmakingService() {}

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

    @Override
    public void addPlayerToQueue(Player player) {
        if (player == null) {
            logger.warn("Tentativa de adicionar jogador nulo à fila de matchmaking");
            return;
        }
        
        if (!matchmakingQueue.contains(player)) {
            matchmakingQueue.offer(player);
            logger.info("{} entrou na fila de matchmaking", player.getNickname());
        } else {
            logger.debug("{} já está na fila de matchmaking", player.getNickname());
        }
    }

    @Override
    public Optional<Match> findMatch() {
        synchronized (lock) {
            if (matchmakingQueue.size() >= 2) {
                Player player1 = matchmakingQueue.poll();
                Player player2 = matchmakingQueue.poll();

                if (player1 != null && player2 != null) {
                    logger.info("Pareamento encontrado: {} vs {}", player1.getNickname(), player2.getNickname());
                    Match match = new Match(player1, player2);
                    return Optional.of(match);
                } else {
                    logger.warn("Erro ao formar pareamento: um ou ambos os jogadores são nulos");
                }
            }
            return Optional.empty();
        }
    }
}