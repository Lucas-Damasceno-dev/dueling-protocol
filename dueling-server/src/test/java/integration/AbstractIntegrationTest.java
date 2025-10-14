package integration;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest(
    classes = {controller.DuelingProtocolApplication.class, config.TestConfig.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ContextConfiguration(initializers = AbstractIntegrationTest.Initializer.class)
@ExtendWith(TestContainersExtension.class)
public abstract class AbstractIntegrationTest {

    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            TestPropertyValues.of(
                "spring.datasource.url=" + ContainerManager.getPostgresqlContainer().getJdbcUrl(),
                "spring.datasource.username=" + ContainerManager.getPostgresqlContainer().getUsername(),
                "spring.datasource.password=" + ContainerManager.getPostgresqlContainer().getPassword(),
                "spring.redis.host=" + ContainerManager.getRedisContainer().getHost(),
                "spring.redis.port=" + ContainerManager.getRedisContainer().getFirstMappedPort()
            ).applyTo(configurableApplicationContext.getEnvironment());
        }
    }
}