package com.shoplite.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import com.shoplite.security.ApiSecurity;

/** Who may call what in the catalog service. */
@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http) throws Exception {
        ApiSecurity.statelessJwtApi(http)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(ApiSecurity.PUBLIC_PATHS).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/products/**", "/api/categories/**", "/api/shop")
                        .permitAll()
                        .requestMatchers("/api/products/**", "/api/categories/**", "/actuator/**").hasRole("ADMIN")
                        // Called by the order service while a customer checks out. The order service
                        // forwards the customer's own token (token relay), so the role is CUSTOMER.
                        .requestMatchers("/internal/stock/**").hasRole("CUSTOMER")
                        .anyRequest().denyAll());
        return http.build();
    }

}
