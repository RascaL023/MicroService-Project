package com.rascal.course_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import id.rascal.filter.HeaderAuthFilter;
import id.rascal.response_kit.exception.SecurityExceptionHandler;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final HeaderAuthFilter headerAuthFilter;
    private final SecurityExceptionHandler securityExceptionHandler;

    public SecurityConfig(
        HeaderAuthFilter headerAuthFilter,
        SecurityExceptionHandler securityExceptionHandler
    ) {
        this.headerAuthFilter = headerAuthFilter;
        this.securityExceptionHandler = securityExceptionHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
        HttpSecurity httpSecurity
    ) throws Exception {
        return httpSecurity
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .anyRequest().authenticated()
                // .anyRequest().permitAll()
            ).exceptionHandling(ex -> ex
                .authenticationEntryPoint(securityExceptionHandler)
                .accessDeniedHandler(securityExceptionHandler)
            ).addFilterBefore(
                headerAuthFilter, 
                UsernamePasswordAuthenticationFilter.class
            ).build();
    }
    
}
