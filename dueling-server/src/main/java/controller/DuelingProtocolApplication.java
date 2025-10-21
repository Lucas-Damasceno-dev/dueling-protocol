package controller;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import ping.PingServer;
import security.JwtProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
@ComponentScan(basePackages = {"controller", "service", "api", "pubsub", "websocket", "config", "model", "security", "repository"})
@EntityScan(basePackages = {"model"})
@EnableJpaRepositories(basePackages = {"repository"})
@EnableScheduling
public class DuelingProtocolApplication {

    public static void main(String[] args) {
        SpringApplication.run(DuelingProtocolApplication.class, args);
        new Thread(new PingServer(7778)).start();
    }
}