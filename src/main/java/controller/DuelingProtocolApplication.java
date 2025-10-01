package controller;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import ping.PingServer;

@SpringBootApplication
@ComponentScan(basePackages = {"controller", "service", "repository", "api", "pubsub", "websocket", "config"})
public class DuelingProtocolApplication {

    public static void main(String[] args) {
        String profile = System.getProperty("spring.profiles.active");
        if ("client".equals(profile)) {
            // Run the simple command-line client
            client.GameClient.main(args);
        } else {
            // Run the full Spring Boot server application
            SpringApplication.run(DuelingProtocolApplication.class, args);
            new Thread(new PingServer(7778)).start();
        }
    }
}