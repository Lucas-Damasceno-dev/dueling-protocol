package com.dueling.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;
import java.util.List;

@Component
public class CustomJwtAuthGatewayFilterFactory extends AbstractGatewayFilterFactory<CustomJwtAuthGatewayFilterFactory.Config> {

    @Value("${jwt.secret:mySecretKeyForDuelingProtocol}")
    private String jwtSecret;

    public CustomJwtAuthGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            
            String path = request.getURI().getPath();
            if (isPublicPath(path)) {
                return chain.filter(exchange);
            }

            String token = null;
            String authHeader = null;
            boolean isWebSocket = path.startsWith("/ws");

            // For WebSocket, prioritize query param
            if (isWebSocket) {
                List<String> tokenParams = request.getQueryParams().get("token");
                if (tokenParams != null && !tokenParams.isEmpty()) {
                    token = tokenParams.get(0);
                    System.out.println("Found token in query parameters for WebSocket: " + (token != null && token.length() > 10 ? token.substring(0, 10) + "..." : "null"));
                }
            }

            // If token is not found yet (or not a WebSocket request), try the Authorization header
            if (token == null) {
                authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    token = authHeader.substring(7);
                }
            }
 
            if (token == null) {
                System.out.println("No authentication token found in header or query parameters for path: " + request.getURI().getPath());
                ServerHttpResponse response = exchange.getResponse();
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                String error = "{\"error\":\"Missing or invalid authentication token\"}";
                DataBuffer buffer = response.bufferFactory().wrap(error.getBytes(StandardCharsets.UTF_8));
                return response.writeWith(Mono.just(buffer));
            }

            try {
                System.out.println("Attempting to validate JWT token for path: " + request.getURI().getPath());
                Claims claims = parseJwtClaims(token);
                System.out.println("JWT token validated successfully for user: " + claims.getSubject());
                
                String roles = claims.get("roles", String.class);
                if (roles == null) {
                    roles = "USER"; // Default role if not specified in token
                }
                
                // Repassar o cabeçalho Authorization original e adicionar os cabeçalhos customizados
                ServerHttpRequest.Builder requestBuilder = request.mutate()
                        .header("X-User-Id", claims.getSubject())
                        .header("X-User-Roles", roles);
                
                // Se tivermos o cabeçalho Authorization original, repassá-lo também
                if (authHeader != null) {
                    requestBuilder.header(HttpHeaders.AUTHORIZATION, authHeader);
                }
                
                ServerHttpRequest mutatedRequest = requestBuilder.build();
                
                return chain.filter(exchange.mutate().request(mutatedRequest).build());
            } catch (Exception e) {
                System.out.println("JWT Authentication failed: " + e.getMessage());
                e.printStackTrace(); // Print full stack trace for debugging
                ServerHttpResponse response = exchange.getResponse();
                response.setStatusCode(HttpStatus.FORBIDDEN);
                String error = "{\"error\":\"Invalid or expired JWT token\"}";
                DataBuffer buffer = response.bufferFactory().wrap(error.getBytes(StandardCharsets.UTF_8));
                return response.writeWith(Mono.just(buffer));
            }
        };
    }
    
    private boolean isPublicPath(String path) {
        // List of paths that should bypass authentication
        List<String> publicPaths = List.of(
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/verify",
            "/actuator/health"
        );
        
        // Check if the path starts with any of the public paths
        return publicPaths.stream().anyMatch(path::startsWith);
    }


    private Claims parseJwtClaims(String token) {
        Key key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        Jws<Claims> jws = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
        return jws.getBody();
    }

    public static class Config {
        // Configuration properties if needed
    }
}
