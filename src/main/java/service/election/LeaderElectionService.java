package service.election;

import api.registry.ServerRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Profile("server")
@Service
public class LeaderElectionService {

    private final ServerRegistry serverRegistry;

    @Value("${server.name}")
    private String serverName;
    
    @Value("${server.port}")
    private String serverPort;

    private String selfUrl;

    @Autowired
    public LeaderElectionService(ServerRegistry serverRegistry) {
        this.serverRegistry = serverRegistry;
    }

    private String getSelfUrl() {
        if (selfUrl == null) {
            selfUrl = "http://" + serverName + ":" + serverPort;
        }
        return selfUrl;
    }

    public String getLeader() {
        List<String> servers = new ArrayList<>(serverRegistry.getRegisteredServers());
        servers.add(getSelfUrl());
        Collections.sort(servers);
        if (servers.isEmpty()) return null;
        return servers.get(0);
    }

    public boolean isLeader() {
        String leader = getLeader();
        return leader != null && leader.equals(getSelfUrl());
    }
}