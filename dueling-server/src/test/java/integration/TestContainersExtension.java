package integration;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class TestContainersExtension implements BeforeAllCallback, ExtensionContext.Store.CloseableResource {

    private static boolean started = false;

    @Override
    public void beforeAll(ExtensionContext context) {
        if (!started) {
            started = true;
            ContainerManager.getPostgresqlContainer().start();
            ContainerManager.getRedisContainer().start();
        }
    }

    @Override
    public void close() {
        // This will be called when the root test context is shut down
    }
}
