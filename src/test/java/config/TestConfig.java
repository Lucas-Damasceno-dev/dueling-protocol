package config;

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
public class TestConfig {
}