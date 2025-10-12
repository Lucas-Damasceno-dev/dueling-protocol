package repository;

import model.Match;

import java.util.List;
import java.util.Optional;

public interface MatchRepository {

    void save(Match match);

    Optional<Match> findById(String id);

    List<Match> findAll();

    List<Match> findByServerUrl(String serverUrl);

    void delete(String id);
}
