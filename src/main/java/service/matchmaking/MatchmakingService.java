package service.matchmaking;

import model.Player;
import model.Match;
import java.util.Optional;

public interface MatchmakingService {
    void addPlayerToQueue(Player player);
    Optional<Match> findMatch();
}