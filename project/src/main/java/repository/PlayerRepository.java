package repository;

import model.Player;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public interface PlayerRepository {
    void save(Player player);
    Optional<Player> findById(String id);
    void update(Player player);
}