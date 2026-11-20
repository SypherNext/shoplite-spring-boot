package com.shoplite.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import com.shoplite.security.ApiSecurity;

/** Who may call what in the order service. */
@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity // turns on @PreAuthorize
public class SecurityConfig {

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http) throws Exception {
        ApiSecurity.statelessJwtApi(http)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(ApiSecurity.PUBLIC_PATHS).permitAll()
                        .requestMatchers("/api/admin/**", "/actuator/**").hasRole("ADMIN")
                        .requestMatchers("/api/orders/**", "/api/customers/**").hasRole("CUSTOMER")
                        .anyRequest().denyAll());
        return http.build();
    }

}
