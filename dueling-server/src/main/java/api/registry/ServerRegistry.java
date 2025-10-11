package api.registry;

import org.springframework.stereotype.Component;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Component
public class ServerRegistry {

    private final Set<String> registeredServers = Collections.synchronizedSet(new HashSet<>());

    public void registerServer(String serverUrl) {
        registeredServers.add(serverUrl);
    }

    public Set<String> getRegisteredServers() {
        return Collections.unmodifiableSet(registeredServers);
    }

    public void unregisterServer(String serverUrl) {
        registeredServers.remove(serverUrl);
    }
}
