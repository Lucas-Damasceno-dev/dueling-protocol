package repository;

import model.Player;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryPlayerRepository implements PlayerRepository {
    private final Map<String, Player> players = new HashMap<>();
    
    @Override
    public void save(Player player) {
        players.put(player.getId(), player);
    }
    
    @Override
    public Optional<Player> findById(String id) {
        return Optional.ofNullable(players.get(id));
    }
    
    @Override
    public void update(Player player) {
        players.put(player.getId(), player);
    }
}