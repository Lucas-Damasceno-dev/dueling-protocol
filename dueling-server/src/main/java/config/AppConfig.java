package config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Dueling Protocol API")
                        .version("1.0.0")
                        .description("API REST para o sistema de jogo de cartas multiplayer Dueling Protocol.")
                        .termsOfService("http://example.com/terms/")
                        .license(new License().name("MIT License").url("http://springdoc.org")));
    }
}
