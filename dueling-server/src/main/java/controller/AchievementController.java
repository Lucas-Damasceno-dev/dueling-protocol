package controller;

import model.PlayerAchievement;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import repository.PlayerAchievementRepository;
import repository.PlayerRepository;

import java.util.List;

@RestController
@RequestMapping("/achievements")
public class AchievementController {

    private final PlayerAchievementRepository playerAchievementRepository;
    private final PlayerRepository playerRepository;

    public AchievementController(PlayerAchievementRepository playerAchievementRepository, @Qualifier("playerRepositoryImpl") PlayerRepository playerRepository) {
        this.playerAchievementRepository = playerAchievementRepository;
        this.playerRepository = playerRepository;
    }

    @GetMapping("/{playerId}")
    public List<PlayerAchievement> getPlayerAchievements(@PathVariable String playerId) {
        return playerRepository.findById(playerId)
                .map(playerAchievementRepository::findByPlayer)
                .orElseThrow(() -> new RuntimeException("Player not found"));
    }
}
