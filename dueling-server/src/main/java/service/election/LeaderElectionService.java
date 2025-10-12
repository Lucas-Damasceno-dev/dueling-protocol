package service.election;

import org.redisson.api.RLeaderElector;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Profile("server")
@Service
public class LeaderElectionService {

    private static final Logger logger = LoggerFactory.getLogger(LeaderElectionService.class);
    private static final String LEADER_ELECTION_KEY = "dueling-protocol-leader-election";
    private static final String LEADER_URL_KEY = "leader:url";

    private final RedissonClient redissonClient;
    private final RLeaderElector leaderElector;
    private final String selfUrl;

    public LeaderElectionService(RedissonClient redissonClient,
                                 @Value("${server.name}") String serverName,
                                 @Value("${server.port}") String serverPort) {
        this.redissonClient = redissonClient;
        this.selfUrl = "http://" + serverName + ":" + serverPort;
        this.leaderElector = redissonClient.getLeaderElector(LEADER_ELECTION_KEY);
    }

    @PostConstruct
    public void start() {
        leaderElector.addListener(new org.redisson.api.listener.LeaderElectionListener() {
            @Override
            public void onLeaderElection(String leader) {
                if (selfUrl.equals(leader)) {
                    logger.info("This server ({}) has been elected as the new leader.", selfUrl);
                    redissonClient.getBucket(LEADER_URL_KEY).set(selfUrl);
                } else {
                    logger.info("A new leader has been elected: {}. This server is a follower.", leader);
                }
            }
        });

        // Tenta se tornar o líder
        leaderElector.tryToBecomeLeader(selfUrl);
        logger.info("This server ({}) has joined the leader election.", selfUrl);
    }

    public String getLeader() {
        return (String) redissonClient.getBucket(LEADER_URL_KEY).get();
    }

    public boolean isLeader() {
        return leaderElector.isLeader();
    }
}