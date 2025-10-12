package repository;

import model.Player;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class PlayerRepositoryImpl implements PlayerRepository {
    private final Map<String, Player> playerStore = new ConcurrentHashMap<>();

    @Override
    public void save(Player player) {
        playerStore.put(player.getId(), player);
    }

    @Override
    public Optional<Player> findById(String id) {
        return Optional.ofNullable(playerStore.get(id));
    }

    @Override
    public void update(Player player) {
        playerStore.put(player.getId(), player);
    }
}
