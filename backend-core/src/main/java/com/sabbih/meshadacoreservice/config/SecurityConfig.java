package com.sabbih.meshadacoreservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/ugc/**", "/api/v1/social/**", "/api/v1/products/**").permitAll()
                .anyRequest().permitAll() // Allow all temporarily for development, adjust as needed
            )
            // If OAuth2 login is desired for other endpoints, we can configure it here:
            .oauth2Login(oauth2 -> {});
            
        return http.build();
    }
}
