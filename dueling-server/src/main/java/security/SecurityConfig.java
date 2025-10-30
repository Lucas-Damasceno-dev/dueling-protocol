package security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService userDetailsService;
    
    @Autowired
    private JwtRequestFilter jwtRequestFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    @Order(1) // Prioridade alta para isolar o WebSocket
    public SecurityFilterChain webSocketSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/ws/**") // Aplica esta cadeia de filtros APENAS para /ws/**
            .authorizeHttpRequests(authz -> authz.anyRequest().permitAll()) // Permite todas as requisições de handshake
            .csrf(csrf -> csrf.disable()); // CSRF não é relevante para WebSockets
        return http.build();
    }

    @Bean
    @Order(2) // Prioridade mais baixa para o resto da API
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Desativa CSRF para uma API stateless
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/auth/**", "/auth/**").permitAll()
                .requestMatchers("/api/health", "/health").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/api/servers/**", "/servers/**").permitAll()
                .requestMatchers("/api/matchmaking/**").permitAll() // Allow internal matchmaking communication
                .requestMatchers("/api/lock/**").permitAll() // Allow internal lock communication
                .requestMatchers("/api/trades/**").permitAll() // Allow internal trade communication
                .anyRequest().authenticated() // Todas as outras requisições exigem autenticação
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // API é stateless
            );

        // Adiciona o filtro JWT para validar os tokens nas requisições da API
        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}