package service.achievement;

import model.Achievement;
import model.Player;
import model.PlayerAchievement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import repository.AchievementRepository;
import repository.PlayerAchievementRepository;
import repository.PlayerRepository;

import java.util.List;
import java.util.Optional;

@Service
public class AchievementService {

    private static final Logger logger = LoggerFactory.getLogger(AchievementService.class);

    private final AchievementRepository achievementRepository;
    private final PlayerAchievementRepository playerAchievementRepository;
    private final PlayerRepository playerRepository;

    public AchievementService(AchievementRepository achievementRepository,
                              PlayerAchievementRepository playerAchievementRepository,
                              PlayerRepository playerRepository) {
        this.achievementRepository = achievementRepository;
        this.playerAchievementRepository = playerAchievementRepository;
        this.playerRepository = playerRepository;
    }

    public void updateAchievementProgress(Player player, String triggerType) {
        List<Achievement> achievements = achievementRepository.findByTriggerType(triggerType);

        for (Achievement achievement : achievements) {
            PlayerAchievement playerAchievement = playerAchievementRepository
                    .findByPlayerAndAchievement(player, achievement)
                    .orElseGet(() -> {
                        PlayerAchievement newPa = new PlayerAchievement();
                        newPa.setPlayer(player);
                        newPa.setAchievement(achievement);
                        return newPa;
                    });

            if (!playerAchievement.isUnlocked()) {
                playerAchievement.setProgress(playerAchievement.getProgress() + 1);

                if (playerAchievement.getProgress() >= achievement.getTriggerThreshold()) {
                    playerAchievement.setUnlocked(true);
                    player.setCoins(player.getCoins() + achievement.getRewardCoins());
                    playerRepository.save(player);
                    logger.info("Player {} unlocked achievement: {}", player.getNickname(), achievement.getName());
                    // TODO: Notify player about the unlocked achievement
                }
                playerAchievementRepository.save(playerAchievement);
            }
        }
    }
}
