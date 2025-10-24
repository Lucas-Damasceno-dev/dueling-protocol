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
    
    @Value("${distributed.enabled:true}")
    private boolean distributedEnabled;

    @Autowired
    private ServerApiClient serverApiClient;

    @Autowired
    private ServerRegistry serverRegistry;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        // Use container name for Docker, localhost for local development
        String selfUrl = "http://" + serverName + ":" + serverPort;
        serverRegistry.registerServer(selfUrl);
        logger.info("Registered self as: {}", selfUrl);
        
        // Skip peer registration if running in standalone mode
        if (!distributedEnabled) {
            logger.info("Running in standalone mode - peer registration skipped");
            return;
        }

        // Register all known server peers in Docker environment
        String[] knownServers = {"server-1", "server-2", "server-3", "server-4"};
        for (String peerName : knownServers) {
            if (!peerName.equals(serverName)) {
                String peerUrl = "http://" + peerName + ":" + serverPort;
                registerPeer(peerUrl, selfUrl);
            }
        }
    }
    
    private void registerPeer(String peerUrl, String selfUrl) {
        logger.info("Will attempt to register with peer: {}", peerUrl);
        
        // Use a new thread to avoid blocking startup
        new Thread(() -> {
            boolean registered = false;
            int retries = 5;
            while (!registered && retries > 0) {
                try {
                    Thread.sleep(3000); // wait 3s before retrying
                    logger.info("Attempting to register with peer at {}", peerUrl);
                    serverRegistry.registerServer(peerUrl);
                    serverApiClient.registerWithServer(peerUrl, selfUrl);
                    registered = true;
                    logger.info("✓ Successfully registered with peer {}", peerUrl);
                } catch (Exception e) {
                    logger.debug("Could not register with peer {} (may not be ready): {}", peerUrl, e.getMessage());
                    retries--;
                }
            }
        }).start();
    }
}
