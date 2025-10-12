package config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Value("${spring.redis.host:#{systemProperties['redis.host'] ?: 'localhost'}}")
    private String redisHost;

    @Value("${spring.redis.port:#{systemProperties['redis.port'] ?: 6379}}")
    private int redisPort;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        String redisAddress = "redis://" + redisHost + ":" + redisPort;
        System.out.println("--- Configuring Redisson with address: " + redisAddress + " ---");
        config.useSingleServer()
                .setAddress(redisAddress);

        try {
            return Redisson.create(config);
        } catch (Exception e) {
            System.err.println("!!! Failed to create Redisson client: " + e.getMessage());
            throw e;
        }
    }
}