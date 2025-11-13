package config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.redisson.config.SentinelServersConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test && !local-dev && !local-distributed")  // Não usar em testes ou desenvolvimento local
public class RedissonSentinelConfig {

    private static final Logger logger = LoggerFactory.getLogger(RedissonSentinelConfig.class);

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(@Value("${spring.redis.sentinel.master:mymaster}") String masterName,
                                         @Value("${spring.redis.sentinel.nodes:redis-sentinel-1:26379,redis-sentinel-2:26379,redis-sentinel-3:26379}") String sentinelNodes) {
        Config config = new Config();
        
        // Configure ObjectMapper to handle JPA entities and collections properly
        ObjectMapper objectMapper = new ObjectMapper();
        
        // Hibernate module
        Hibernate6Module hibernate6Module = new Hibernate6Module();
        hibernate6Module.configure(Hibernate6Module.Feature.FORCE_LAZY_LOADING, false);
        hibernate6Module.configure(Hibernate6Module.Feature.SERIALIZE_IDENTIFIER_FOR_LAZY_NOT_LOADED_OBJECTS, true);
        objectMapper.registerModule(hibernate6Module);
        
        // Configurações para melhor serialização de listas e coleções
        objectMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.setVisibility(
            objectMapper.getSerializationConfig()
                .getDefaultVisibilityChecker()
                .withFieldVisibility(com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY)
                .withSetterVisibility(com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY)
                .withCreatorVisibility(com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY)
        );
        
        // Using JsonJacksonCodec with custom ObjectMapper for better compatibility with JPA entities
        config.setCodec(new JsonJacksonCodec(objectMapper));
        
        logger.info("--- Configuring Redisson with Sentinel (Enhanced JSON Codec) ---");
        
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
            logger.error("!!! Failed to create Redisson client with Sentinel: {}", e.getMessage());
            throw e;
        }
    }
}