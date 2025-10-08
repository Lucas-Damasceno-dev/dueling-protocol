package config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@TestConfiguration
@ComponentScan(basePackages = {
    "controller", 
    "service", 
    "repository", 
    "api", 
    "pubsub", 
    "websocket", 
    "config",
    "model",
    "security"
})
@EnableJpaRepositories(basePackages = {"repository", "model"})
// Disable Hibernate second-level cache for tests
@EnableAutoConfiguration(exclude = {
    org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration.class
})
public class TestConfig {
}