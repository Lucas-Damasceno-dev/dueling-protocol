package integration;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public class ContainerManager {

    private static final PostgreSQLContainer<?> postgresqlContainer;
    private static final GenericContainer<?> redisContainer;

    static {
        postgresqlContainer = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:15")
                .asCompatibleSubstituteFor("postgres")
        ).withDatabaseName("dueling_test")
         .withUsername("dueling_user")
         .withPassword("dueling_password");

        redisContainer = new GenericContainer<>(
            DockerImageName.parse("redis:7-alpine")
        ).withExposedPorts(6379);

        postgresqlContainer.start();
        redisContainer.start();
    }

    public static PostgreSQLContainer<?> getPostgresqlContainer() {
        return postgresqlContainer;
    }

    public static GenericContainer<?> getRedisContainer() {
        return redisContainer;
    }
}
