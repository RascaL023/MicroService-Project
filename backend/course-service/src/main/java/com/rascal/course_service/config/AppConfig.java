package com.rascal.course_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class AppConfig {

    @Value("${services.url.user}")
    private String usersUrl;

    @Bean
    public WebClient usersWebClient() {
        return WebClient.builder()
            .baseUrl(usersUrl).build();
    }
    
}
