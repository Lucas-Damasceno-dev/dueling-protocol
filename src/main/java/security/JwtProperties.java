package security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String secret;
    private long expiration;

    // Getters
    public String getSecret() {
        return secret;
    }

    public long getExpiration() {
        return expiration;
    }

    // Setters
    public void setSecret(String secret) {
        this.secret = secret;
    }

    public void setExpiration(long expiration) {
        this.expiration = expiration;
    }
}