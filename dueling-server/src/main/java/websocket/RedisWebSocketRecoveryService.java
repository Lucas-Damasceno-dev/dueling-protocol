package websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Monitora a conectividade do Redis e reconecta sessões após failover
 */
@Component
public class RedisWebSocketRecoveryService {
    
    private static final Logger logger = LoggerFactory.getLogger(RedisWebSocketRecoveryService.class);
    
    private final WebSocketSessionManager sessionManager;
    private final RedisConnectionFactory redisConnectionFactory;
    
    private boolean wasRedisAvailable = true;
    private long lastFailoverTime = 0;
    
    @Autowired
    public RedisWebSocketRecoveryService(
            WebSocketSessionManager sessionManager,
            RedisConnectionFactory redisConnectionFactory) {
        this.sessionManager = sessionManager;
        this.redisConnectionFactory = redisConnectionFactory;
    }
    
    /**
     * Verifica a cada 5 segundos se o Redis está disponível
     * e reconecta sessões se detectar uma recuperação após falha
     */
    @Scheduled(fixedDelay = 5000, initialDelay = 10000)
    public void checkRedisAndRecover() {
        boolean isRedisAvailable = checkRedisConnection();
        
        // Detecta recuperação após falha
        if (!wasRedisAvailable && isRedisAvailable) {
            long timeSinceFailover = System.currentTimeMillis() - lastFailoverTime;
            logger.info("Redis reconnected after {}ms! Attempting session recovery...", timeSinceFailover);
            
            try {
                sessionManager.reconnectSessionsFromRedis();
                logger.info("Session recovery completed successfully");
            } catch (Exception e) {
                logger.error("Failed to recover sessions after Redis reconnection: {}", e.getMessage());
            }
        }
        
        // Detecta nova falha
        if (wasRedisAvailable && !isRedisAvailable) {
            lastFailoverTime = System.currentTimeMillis();
            logger.warn("Redis connection lost! Sessions will continue with local storage");
        }
        
        wasRedisAvailable = isRedisAvailable;
    }
    
    private boolean checkRedisConnection() {
        try {
            // Tenta obter uma conexão do pool
            redisConnectionFactory.getConnection().ping();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Retorna true se o Redis está atualmente disponível
     */
    public boolean isRedisAvailable() {
        return wasRedisAvailable;
    }
}
