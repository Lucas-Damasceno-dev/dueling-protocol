package controller;

import model.PlayerRanking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import repository.PlayerRankingRepository;

@RestController
@RequestMapping("/leaderboard")
public class LeaderboardController {

    private final PlayerRankingRepository playerRankingRepository;

    public LeaderboardController(PlayerRankingRepository playerRankingRepository) {
        this.playerRankingRepository = playerRankingRepository;
    }

    @GetMapping
    public Page<PlayerRanking> getLeaderboard(@PageableDefault(sort = "eloRating", direction = Sort.Direction.DESC) Pageable pageable) {
        return playerRankingRepository.findAll(pageable);
    }
}
