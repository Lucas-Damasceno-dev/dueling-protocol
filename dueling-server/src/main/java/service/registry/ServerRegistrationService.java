package service.registry;

import api.ServerApiClient;
import api.registry.ServerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Profile("server")
@Service
public class ServerRegistrationService implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger logger = LoggerFactory.getLogger(ServerRegistrationService.class);

    @Value("${server.name}")
    private String serverName;

    @Value("${server.port}")
    private int serverPort;

    @Autowired
    private ServerApiClient serverApiClient;

    @Autowired
    private ServerRegistry serverRegistry;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        String selfUrl = "http://" + serverName + ":" + serverPort;
        serverRegistry.registerServer(selfUrl);

        String peerName = "server-1".equals(serverName) ? "server-2" : "server-1";
        String peerUrl = "http://" + peerName + ":8080";
        
        // Use a new thread to avoid blocking startup
        new Thread(() -> {
            boolean registered = false;
            int retries = 5;
            while (!registered && retries > 0) {
                try {
                    Thread.sleep(3000); // wait 3s before retrying
                    logger.info("Attempting to register with peer at {}", peerUrl);
                    serverApiClient.registerWithServer(peerUrl, selfUrl);
                    registered = true;
                    logger.info("Successfully registered with peer {}", peerUrl);
                } catch (Exception e) {
                    logger.warn("Could not register with peer {}. Retrying...", peerUrl);
                    retries--;
                }
            }
        }).start();
    }
}
