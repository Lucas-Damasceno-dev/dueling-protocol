package controller;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import ping.PingServer;

@SpringBootApplication
@ComponentScan(basePackages = {"controller", "service", "repository", "api", "pubsub", "websocket", "config"})
public class DuelingProtocolApplication {

    public static void main(String[] args) {
        // This allows running the client or server from the same JAR.
        String profile = System.getProperty("spring.profiles.active");
        if ("client".equals(profile)) {
            client.GameClient.main(args);
        } else {
            SpringApplication.run(DuelingProtocolApplication.class, args);
            new Thread(new PingServer(7778)).start();
        }
    }
}