package service.lock;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class LockService {

    private static final Logger logger = LoggerFactory.getLogger(LockService.class);
    private static final String PURCHASE_LOCK = "purchaseLock";
    private final RedissonClient redissonClient;

    public LockService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    public boolean acquire() {
        RLock lock = redissonClient.getLock(PURCHASE_LOCK);
        try {
            // Tenta adquirir o lock, esperando no máximo 10 segundos.
            // O lock será liberado automaticamente após 60 segundos se algo der errado.
            return lock.tryLock(10, 60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public void release() {
        RLock lock = redissonClient.getLock(PURCHASE_LOCK);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    /**
     * Forcibly unlocks the distributed purchase lock. This method should only be called
     * by a newly elected leader to clean up a potentially orphaned lock left by a
     * previous leader that failed. Forcing an unlock is an aggressive action and should
     * be used with caution, as the lock could theoretically be held by a slow but
     * still-alive process. The primary safety mechanism remains the lock's lease time.
     */
    public void cleanOrphanedPurchaseLock() {
        RLock lock = redissonClient.getLock(PURCHASE_LOCK);
        if (lock.isLocked()) {
            logger.warn("Purchase lock is currently locked. Forcing unlock as the new leader. " +
                        "This assumes the previous holder has failed.");
            lock.forceUnlock();
        }
    }
}