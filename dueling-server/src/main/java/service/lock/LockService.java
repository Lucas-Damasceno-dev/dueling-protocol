package service.lock;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class LockService {

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
}