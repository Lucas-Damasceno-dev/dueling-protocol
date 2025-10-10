package repository;

import model.GameSession;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class RedisGameSessionRepository implements GameSessionRepository {

    private final RedissonClient redissonClient;
    private static final String GAME_SESSION_KEY_PREFIX = "game_session:";

    public RedisGameSessionRepository(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public void save(GameSession gameSession) {
        RMap<String, GameSession> map = redissonClient.getMap(GAME_SESSION_KEY_PREFIX + gameSession.getMatchId());
        map.put("data", gameSession);
    }

    @Override
    public Optional<GameSession> findById(String matchId) {
        RMap<String, GameSession> map = redissonClient.getMap(GAME_SESSION_KEY_PREFIX + matchId);
        return Optional.ofNullable(map.get("data"));
    }

    @Override
    public void deleteById(String matchId) {
        redissonClient.getMap(GAME_SESSION_KEY_PREFIX + matchId).delete();
    }
}
