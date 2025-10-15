package repository;

import model.GameSession;
import java.util.Optional;

public interface GameSessionRepository {
    void save(GameSession gameSession);
    Optional<GameSession> findById(String matchId);
    void deleteById(String matchId);
}
