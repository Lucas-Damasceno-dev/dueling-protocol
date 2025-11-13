package controller;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.EnableAsync;
import ping.PingServer;
import security.JwtProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
@ComponentScan(basePackages = {"controller", "service", "api", "pubsub", "websocket", "config", "model", "security", "repository"})
@EntityScan(basePackages = {"model"})
@EnableJpaRepositories(basePackages = {"repository"})
@EnableScheduling
@EnableAsync
public class DuelingProtocolApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(DuelingProtocolApplication.class, args);
        Environment env = context.getEnvironment();
        
        // Only start PingServer if not in local-dev mode
        String pingEnabled = env.getProperty("ping.server.enabled", "true");
        if ("true".equals(pingEnabled)) {
            new Thread(new PingServer(7778)).start();
        }
    }
}