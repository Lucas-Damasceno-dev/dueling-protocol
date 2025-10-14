package config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SentinelServersConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import java.util.Arrays;
import java.util.List;

@Configuration
@Profile("!test")
public class RedissonConfig {

    private static final Logger logger = LoggerFactory.getLogger(RedissonConfig.class);

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(@Value("${spring.redis.sentinel.master:mymaster}") String masterName,
                                         @Value("${spring.redis.sentinel.nodes:redis-sentinel-1:26379,redis-sentinel-2:26379,redis-sentinel-3:26379}") String sentinelNodes) {
        Config config = new Config();
        
        logger.info("--- Configuring Redisson with Sentinel ---");
        
        SentinelServersConfig sentinelConfig = config.useSentinelServers();
        sentinelConfig.setMasterName(masterName);
        
        // Split the sentinel nodes and add them to the configuration
        String[] nodes = sentinelNodes.split(",");
        for (String node : nodes) {
            String trimmedNode = node.trim();
            if (!trimmedNode.startsWith("redis://")) {
                trimmedNode = "redis://" + trimmedNode;
            }
            sentinelConfig.addSentinelAddress(trimmedNode);
        }

        try {
            return Redisson.create(config);
        } catch (Exception e) {
            logger.error("!!! Failed to create Redisson client: {}", e.getMessage());
            throw e;
        }
    }
}