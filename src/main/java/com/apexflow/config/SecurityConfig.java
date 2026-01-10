package com.apexflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Disabilitiamo CSRF per permettere i test
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll() // 1. Permetti a CHIUNQUE di accedere
            );
        
        return http.build();
    }
}