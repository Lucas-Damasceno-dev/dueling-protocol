package repository;

import api.ServerApiClient;
import api.registry.ServerRegistry;
import model.Player;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Profile;

@Repository
@Primary
public class DistributedPlayerRepository implements PlayerRepository {

    private final PlayerRepository localRepository;
    private final ServerRegistry serverRegistry;
    private final ServerApiClient serverApiClient;

    @Value("${server.name:server-1}")
    private String serverName;
    
    @Value("${server.port:8080}")
    private String serverPort;

    private String selfUrl;

    @Autowired
    public DistributedPlayerRepository(@Qualifier("playerRepositoryJson") PlayerRepository localRepository,
                                       ServerRegistry serverRegistry,
                                       ServerApiClient serverApiClient) {
        this.localRepository = localRepository;
        this.serverRegistry = serverRegistry;
        this.serverApiClient = serverApiClient;
    }
    
    private String getSelfUrl() {
        if (selfUrl == null) {
            selfUrl = "http://" + serverName + ":" + serverPort;
        }
        return selfUrl;
    }

    private String getServerForPlayer(String playerId) {
        List<String> servers = new ArrayList<>(serverRegistry.getRegisteredServers());
        if (servers.isEmpty()) {
            return getSelfUrl();
        }
        servers.add(getSelfUrl());
        servers.sort(String::compareTo);

        int serverIndex = Math.abs(playerId.hashCode() % servers.size());
        return servers.get(serverIndex);
    }

    @Override
    public void save(Player player) {
        String responsibleServer = getServerForPlayer(player.getId());
        if (getSelfUrl().equals(responsibleServer)) {
            localRepository.save(player);
        } else {
            serverApiClient.savePlayer(responsibleServer, player);
        }
    }

    @Override
    public Optional<Player> findById(String id) {
        String responsibleServer = getServerForPlayer(id);
        if (getSelfUrl().equals(responsibleServer)) {
            return localRepository.findById(id);
        } else {
            return Optional.ofNullable(serverApiClient.getPlayer(responsibleServer, id));
        }
    }

    @Override
    public void update(Player player) {
        save(player);
    }
}
