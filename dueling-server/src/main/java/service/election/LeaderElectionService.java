package service.election;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import service.lock.LockService;

import jakarta.annotation.PostConstruct;

@Profile("server")
@Service
public class LeaderElectionService {

    private static final Logger logger = LoggerFactory.getLogger(LeaderElectionService.class);
    private static final String LEADER_ELECTION_KEY = "dueling-protocol-leader-election";
    private static final String LEADER_URL_KEY = "leader:url";

    private final RedissonClient redissonClient;
    private final String selfUrl;
    private final LockService lockService;
    private RLock leaderLock;

    public LeaderElectionService(RedissonClient redissonClient,
                                 @Value("${server.name}") String serverName,
                                 @Value("${server.port}") String serverPort,
                                 LockService lockService) {
        this.redissonClient = redissonClient;
        this.selfUrl = "http://" + serverName + ":" + serverPort;
        this.leaderLock = redissonClient.getLock(LEADER_ELECTION_KEY);
        this.lockService = lockService;
    }

    @PostConstruct
    public void start() {
        // Attempt to become leader by acquiring the leader lock
        tryToBecomeLeader();
        logger.info("This server ({}) has joined the leader election.", selfUrl);
    }

    private void tryToBecomeLeader() {
        // Use a background task to periodically try to acquire the leader lock
        Thread leaderThread = new Thread(() -> {
            while (true) {
                try {
                    // Attempt to acquire the leader lock with a timeout
                    if (leaderLock.tryLock()) {
                        logger.info("This server ({}) has been elected as the new leader.", selfUrl);
                        redissonClient.getBucket(LEADER_URL_KEY).set(selfUrl);

                        // Clean up potentially orphaned locks from a previous leader
                        lockService.cleanOrphanedPurchaseLock();

                        // Keep the lock as long as we're running
                        try {
                            Thread.sleep(Long.MAX_VALUE); // Keep the lock indefinitely (until interrupted)
                        } catch (InterruptedException e) {
                            logger.warn("Leader thread interrupted, releasing lock");
                            leaderLock.unlock();
                            break;
                        }
                    } else {
                        logger.debug("Failed to acquire leader lock, another server is the leader");
                        Thread.sleep(5000); // Wait 5 seconds before trying again
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("Leader election thread interrupted");
                    break;
                } catch (Exception e) {
                    logger.error("Error during leader election: {}", e.getMessage(), e);
                    try {
                        Thread.sleep(5000); // Wait 5 seconds before retrying
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        });
        leaderThread.setDaemon(true);
        leaderThread.start();
    }

    public String getLeader() {
        return (String) redissonClient.getBucket(LEADER_URL_KEY).get();
    }

    public boolean isLeader() {
        return leaderLock.isHeldByCurrentThread();
    }

    public String getSelfUrl() {
        return selfUrl;
    }
}