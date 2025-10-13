package config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import javax.sql.DataSource;
import repository.PlayerRepository;
import repository.PlayerRepositoryImpl;

import controller.GameFacade;
import model.Player;
import model.User;

import javax.sql.DataSource;
import java.util.Optional;
import java.util.Properties;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.anyString;

@TestConfiguration
@ComponentScan(
    basePackages = {
        "controller", 
        "service", 
        "repository", // Include repository package so repositories are discovered
        "api", 
        "pubsub", 
        "websocket", 
        "config",
        "model",
        "security"
    },
    // Exclude the other PlayerRepository implementations and other problematic implementations to avoid conflicts
    excludeFilters = {
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE, 
            classes = { 
                repository.DistributedPlayerRepository.class,
                repository.PlayerRepositoryJson.class,
                repository.PlayerRepositoryPostgreSQL.class,
                repository.InMemoryPlayerRepository.class,
                config.RedissonConfig.class,
                service.election.LeaderElectionService.class, // Exclude the real LeaderElectionService
                controller.GameFacade.class // Exclude the real GameFacade
            }
        )
    }
)
@EntityScan(basePackages = {"model"})
@EnableJpaRepositories(basePackages = {"repository"})
@Import(TestRedissonConfig.class) // Import our test Redisson configuration
// Provide test DataSource configuration for JPA to work
public class TestConfig {

    @Value("${spring.datasource.url:jdbc:h2:mem:testdb}")
    private String dataSourceUrl;

    @Value("${spring.datasource.username:sa}")
    private String dataSourceUsername;

    @Value("${spring.datasource.password:}")
    private String dataSourcePassword;

    @Value("${spring.datasource.driver-class-name:org.h2.Driver}")
    private String dataSourceDriverClassName;

    @Bean
    @Primary
    public PlayerRepository playerRepository() {
        return createMockPlayerRepository();
    }
    
    @Bean("playerRepositoryImpl") // This matches the qualifier used in AuthenticationService
    public PlayerRepository playerRepositoryImpl() {
        return createMockPlayerRepository();
    }
    
    private PlayerRepository createMockPlayerRepository() {
        PlayerRepository mockPlayerRepository = mock(PlayerRepository.class);
        
        // Configure mock to return a Player for any ID that matches the pattern of unique test data
        // This ensures that our UUID-based player IDs will be found
        when(mockPlayerRepository.findById(anyString())).thenAnswer(invocation -> {
            String id = invocation.getArgument(0);
            // Check if it's one of the explicitly mocked IDs first (to maintain existing behavior)
            if (id.equals("player1") || id.equals("player2") || id.equals("player3") || id.equals("player4") ||
                id.equals("player1_fr") || id.equals("player2_fr") || id.equals("player1_reg_succ") || 
                id.equals("player2_log_succ") || id.equals("player2_log_succ_new") || id.equals("player3_fr_accept") || 
                id.equals("player3_fr_accept_new") || id.equals("player4_fr_accept") || id.equals("player1_fr_send") || 
                id.equals("player1_fr_send_new") || id.equals("player2_fr_target") || id.equals("nonexistentplayer_pnf")) {
                
                // Use the specific mock behavior
                switch (id) {
                    case "player1": return Optional.of(new Player("player1", "testplayer"));
                    case "player2": return Optional.of(new Player("player2", "loginuser"));
                    case "player3": return Optional.of(new Player("player3", "user3"));
                    case "player4": return Optional.of(new Player("player4", "user4"));
                    case "player1_fr": return Optional.of(new Player("player1_fr", "user1_fr"));
                    case "player2_fr": return Optional.of(new Player("player2_fr", "user2_fr"));
                    case "player1_reg_succ": return Optional.of(new Player("player1_reg_succ", "testplayer_reg_succ"));
                    case "player2_log_succ": return Optional.of(new Player("player2_log_succ", "loginuser_log_succ"));
                    case "player2_log_succ_new": return Optional.of(new Player("player2_log_succ_new", "loginuser_log_succ_new"));
                    case "player3_fr_accept": return Optional.of(new Player("player3_fr_accept", "user3_fr_accept"));
                    case "player3_fr_accept_new": return Optional.of(new Player("player3_fr_accept_new", "user3_fr_accept_new"));
                    case "player4_fr_accept": return Optional.of(new Player("player4_fr_accept", "user4_fr_accept"));
                    case "player1_fr_send": return Optional.of(new Player("player1_fr_send", "user1_fr_send"));
                    case "player1_fr_send_new": return Optional.of(new Player("player1_fr_send_new", "user1_fr_send_new"));
                    case "player2_fr_target": return Optional.of(new Player("player2_fr_target", "user2_fr_target"));
                    case "nonexistentplayer_pnf": return Optional.empty(); // This should return empty for non-existent player
                    default: return Optional.of(new Player(id, "testuser_" + id));
                }
            } else {
                // For unique test IDs (which will contain UUID parts), return a generic player
                // But if the ID contains "nonexistent" or "pnf", return empty to simulate not found
                if (id.contains("nonexistent") || id.contains("pnf")) {
                    return Optional.empty();
                } else {
                    return Optional.of(new Player(id, "testuser_" + id));
                }
            }
        });

        return mockPlayerRepository;
    }

    @Bean
    @Primary
    public service.election.LeaderElectionService leaderElectionService() {
        return org.mockito.Mockito.mock(service.election.LeaderElectionService.class);
    }

    @Bean
    @Primary
    public GameFacade gameFacade() {
        return org.mockito.Mockito.mock(GameFacade.class);
    }

    @Bean
    @Primary
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        config.setDriverClassName("org.h2.Driver");
        config.setUsername("sa");
        config.setPassword("");
        config.setMaximumPoolSize(5);
        config.setConnectionTestQuery("SELECT 1");
        config.addDataSourceProperty("MINIMUM_IDLE", "1");
        return new HikariDataSource(config);
    }

    @Bean
    @Primary
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("model");

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);

        Properties properties = new Properties();
        properties.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        properties.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        properties.setProperty("hibernate.hbm2ddl.use_jdbc_metadata_defaults", "false");
        em.setJpaProperties(properties);

        return em;
    }

    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(LocalContainerEntityManagerFactoryBean entityManagerFactory) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory.getObject());

        return transactionManager;
    }
}