package config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
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
        
        logger.info("--- Configuring Redisson with Single Server (Enhanced JSON Codec) ---");
        
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