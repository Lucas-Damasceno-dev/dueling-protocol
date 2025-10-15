package repository;

import model.PlayerRanking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlayerRankingRepository extends JpaRepository<PlayerRanking, String> {
}
