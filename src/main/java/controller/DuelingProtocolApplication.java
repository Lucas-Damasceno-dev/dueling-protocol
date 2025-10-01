package controller;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import ping.PingServer;

@SpringBootApplication
@ComponentScan(basePackages = {"controller", "service", "repository", "api", "pubsub", "websocket", "config"})
public class DuelingProtocolApplication {

    public static void main(String[] args) {
        SpringApplication.run(DuelingProtocolApplication.class, args);
        new Thread(new PingServer(7778)).start();
    }
}
