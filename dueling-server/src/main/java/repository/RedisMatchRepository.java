package repository;

import model.Match;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class RedisMatchRepository implements MatchRepository {

    private static final String MATCH_KEY = "match:";
    private final RMap<String, Match> matches;

    public RedisMatchRepository(RedissonClient redissonClient) {
        this.matches = redissonClient.getMap(MATCH_KEY);
    }

    @Override
    public void save(Match match) {
        matches.put(match.getId(), match);
    }

    @Override
    public Optional<Match> findById(String id) {
        return Optional.ofNullable(matches.get(id));
    }

    @Override
    public List<Match> findAll() {
        return List.copyOf(matches.values());
    }

    @Override
    public List<Match> findByServerUrl(String serverUrl) {
        return matches.values().stream()
                .filter(match -> serverUrl.equals(match.getServerUrl()))
                .collect(Collectors.toList());
    }

    @Override
    public void delete(String id) {
        matches.remove(id);
    }
}
