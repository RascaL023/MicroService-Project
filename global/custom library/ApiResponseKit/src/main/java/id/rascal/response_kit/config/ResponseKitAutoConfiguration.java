package id.rascal.response_kit.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import id.rascal.response_kit.exception.GlobalExceptionHandler;
import id.rascal.response_kit.exception.SecurityExceptionHandler;

import tools.jackson.databind.ObjectMapper;

@AutoConfiguration
public class ResponseKitAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    @Bean
    @ConditionalOnClass(name = "org.springframework.security.web.AuthenticationEntryPoint")
    @ConditionalOnMissingBean
    public SecurityExceptionHandler securityExceptionHandler(ObjectMapper objectMapper) {
        return new SecurityExceptionHandler(objectMapper);
    }

}
