package config;

import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.mockito.Mockito.*;

/**
 * Test configuration for Redisson client that provides a mock implementation
 * to avoid connection attempts to real Redis server during tests.
 */
@TestConfiguration
public class TestRedissonConfig {

    /**
     * Provides a mock RedissonClient for testing to avoid connection attempts to real Redis server
     */
    @Bean
    @Primary
    public RedissonClient redissonClient() {
        RedissonClient mockClient = mock(RedissonClient.class);
        
        // Mock the methods that are actually used by the application
        when(mockClient.getMap(anyString())).thenReturn(mock(org.redisson.api.RMap.class));
        when(mockClient.getBucket(anyString())).thenReturn(mock(org.redisson.api.RBucket.class));
        when(mockClient.getLock(anyString())).thenReturn(mock(org.redisson.api.RLock.class));
        
        return mockClient;
    }
}