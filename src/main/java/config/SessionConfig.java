package config;

import org.springframework.context.annotation.Configuration;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

@Configuration
@EnableRedisHttpSession
public class SessionConfig {
    // This configuration enables Spring Session with Redis for HTTP sessions
    // For WebSocket sessions, we'll need to implement a custom solution that stores
    // session data in Redis to enable horizontal scaling
}