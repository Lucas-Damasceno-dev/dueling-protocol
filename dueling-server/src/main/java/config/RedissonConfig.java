package config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
public class RedissonConfig {

    private static final Logger logger = LoggerFactory.getLogger(RedissonConfig.class);

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(@Value("${spring.redis.host:#{systemProperties['redis.host'] ?: 'localhost'}}") String redisHost,
                                         @Value("${spring.redis.port:#{systemProperties['redis.port'] ?: 6379}}") int redisPort) {
        Config config = new Config();
        String redisAddress = "redis://" + redisHost + ":" + redisPort;
        logger.info("--- Configuring Redisson with address: {} ---", redisAddress);
        config.useSingleServer()
                .setAddress(redisAddress);

        try {
            return Redisson.create(config);
        } catch (Exception e) {
            logger.error("!!! Failed to create Redisson client: {}", e.getMessage());
            throw e;
        }
    }
}