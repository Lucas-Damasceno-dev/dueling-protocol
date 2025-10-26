package config;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {
    "controller",
    "dto", 
    "model", 
    "repository", 
    "security", 
    "service",
    "api",
    "pubsub",
    "websocket",
    "config"
})
@EntityScan(basePackages = "model")
// Note: We're NOT enabling JPA repositories here to use in-memory implementations
public class TestDuelingProtocolApplication {
    // Test-specific application class without JPA repositories enabled
}