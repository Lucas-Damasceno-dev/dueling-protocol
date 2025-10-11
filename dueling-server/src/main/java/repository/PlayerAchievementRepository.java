package repository;

import model.Player;
import model.Achievement;
import model.PlayerAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlayerAchievementRepository extends JpaRepository<PlayerAchievement, Long> {
    Optional<PlayerAchievement> findByPlayerAndAchievement(Player player, Achievement achievement);
    List<PlayerAchievement> findByPlayer(Player player);
}
