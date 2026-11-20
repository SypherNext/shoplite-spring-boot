package com.shoplite.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import com.shoplite.security.ApiSecurity;

/**
 * Only signed-in customers may use the assistant and its MCP endpoint: every answer costs model time,
 * and the tools read the catalog on the caller's behalf.
 */
@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http) throws Exception {
        ApiSecurity.statelessJwtApi(http)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(ApiSecurity.PUBLIC_PATHS).permitAll()
                        .requestMatchers("/api/assistant/**", "/mcp", "/mcp/**").hasRole("CUSTOMER")
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        .anyRequest().denyAll());
        return http.build();
    }

}
