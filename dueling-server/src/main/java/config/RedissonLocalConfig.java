package config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"local-dev", "local-distributed"})  // Perfil para desenvolvimento local (com ou sem distributed)
public class RedissonLocalConfig {

    private static final Logger logger = LoggerFactory.getLogger(RedissonLocalConfig.class);

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(@Value("${spring.redis.host:localhost}") String redisHost,
                                         @Value("${spring.redis.port:6379}") int redisPort) {
        Config config = new Config();
        
        logger.info("--- Configuring Redisson with Single Server (Local Dev) ---");
        
        String address = "redis://" + redisHost + ":" + redisPort;
        SingleServerConfig singleConfig = config.useSingleServer();
        singleConfig.setAddress(address);
        singleConfig.setConnectionMinimumIdleSize(1);
        singleConfig.setConnectionPoolSize(4);

        try {
            return Redisson.create(config);
        } catch (Exception e) {
            logger.error("!!! Failed to create Redisson client with Single Server: {}", e.getMessage());
            throw e;
        }
    }
}