package service.ranking;

import model.Player;
import model.PlayerRanking;
import org.springframework.stereotype.Service;
import repository.PlayerRankingRepository;

@Service
public class RankingService {

    private static final int K_FACTOR = 32;
    private final PlayerRankingRepository playerRankingRepository;

    public RankingService(PlayerRankingRepository playerRankingRepository) {
        this.playerRankingRepository = playerRankingRepository;
    }

    public void updateEloRatings(Player winner, Player loser) {
        PlayerRanking winnerRanking = playerRankingRepository.findById(winner.getId()).orElse(null);
        PlayerRanking loserRanking = playerRankingRepository.findById(loser.getId()).orElse(null);

        if (winnerRanking == null) {
            winnerRanking = new PlayerRanking(winner);
        }
        if (loserRanking == null) {
            loserRanking = new PlayerRanking(loser);
        }

        double winnerExpected = calculateExpectedScore(winnerRanking.getEloRating(), loserRanking.getEloRating());
        double loserExpected = calculateExpectedScore(loserRanking.getEloRating(), winnerRanking.getEloRating());

        int winnerNewRating = (int) (winnerRanking.getEloRating() + K_FACTOR * (1 - winnerExpected));
        int loserNewRating = (int) (loserRanking.getEloRating() + K_FACTOR * (0 - loserExpected));

        winnerRanking.setEloRating(winnerNewRating);
        loserRanking.setEloRating(loserNewRating);

        playerRankingRepository.save(winnerRanking);
        playerRankingRepository.save(loserRanking);
    }

    private double calculateExpectedScore(int playerRating, int opponentRating) {
        return 1.0 / (1.0 + Math.pow(10.0, (double) (opponentRating - playerRating) / 400.0));
    }
}
