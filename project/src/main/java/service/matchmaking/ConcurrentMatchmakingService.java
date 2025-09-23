package service.matchmaking;

import model.Player;
import model.Match;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ConcurrentMatchmakingService implements MatchmakingService {

    private static volatile ConcurrentMatchmakingService instance;
    private final Queue<Player> matchmakingQueue = new ConcurrentLinkedQueue<>();
    private final Object lock = new Object(); // Objeto de lock para a operação de pareamento

    private ConcurrentMatchmakingService() {}

    // Singleton com Double-Checked Locking para thread-safety
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
        if (!matchmakingQueue.contains(player)) {
            matchmakingQueue.offer(player);
            System.out.println(player.getNickname() + " entrou na fila.");
        }
    }

    /**
     * Tenta parear dois jogadores da fila.
     * Esta operação é atômica para evitar race conditions.
     */
    @Override
    public Optional<Match> findMatch() {
        synchronized (lock) {
            if (matchmakingQueue.size() >= 2) {
                Player player1 = matchmakingQueue.poll();
                Player player2 = matchmakingQueue.poll();

                if (player1 != null && player2 != null) {
                    System.out.println("Pareamento encontrado: " + player1.getNickname() + " vs " + player2.getNickname());
                    Match match = new Match(player1, player2);
                    // Aqui você poderia usar o padrão Observer para notificar os jogadores
                    return Optional.of(match);
                }
            }
        }
        return Optional.empty();
    }
}