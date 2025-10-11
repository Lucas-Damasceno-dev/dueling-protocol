package service.registry;

import api.ServerApiClient;
import api.registry.ServerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

import org.springframework.context.annotation.Profile;

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

        // In a real system, you'd get a list of peers from a config server or DNS.
        // For this project, we hardcode the peer based on the server name.
        String peerName = "server-1".equals(serverName) ? "server-2" : "server-1";
        String peerUrl = "http://" + peerName + ":8080";
        
        try {
            logger.info("Attempting to register with peer at {}", peerUrl);
            serverApiClient.registerWithServer(peerUrl, selfUrl);
            logger.info("Successfully registered with peer {}", peerUrl);
        } catch (Exception e) {
            logger.warn("Could not register with peer {}. It might not be up yet. It should register with us later.", peerUrl);
        }
    }
}
