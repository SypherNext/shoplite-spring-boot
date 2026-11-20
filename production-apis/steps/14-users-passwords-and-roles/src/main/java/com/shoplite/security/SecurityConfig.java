package com.shoplite.security;

import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Who may call what. Every request passes through this filter chain before it reaches a controller.
 * Users and their roles come from InMemoryUsersConfig, checked with HTTP Basic.
 */
@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity // turns on @PreAuthorize
@EnableConfigurationProperties(CorsProperties.class)
public class SecurityConfig {

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // Spring Boot's error page: without this, every error would turn into a 403.
                        .requestMatchers("/error").permitAll()
                        // Swagger UI and the OpenAPI JSON.
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // Anyone may browse the catalog.
                        .requestMatchers(HttpMethod.GET, "/api/products/**", "/api/categories/**", "/api/shop")
                        .permitAll()
                        // Changing the catalog and the admin pages need ADMIN.
                        .requestMatchers("/api/products/**", "/api/categories/**", "/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/orders/**", "/api/customers/**").hasRole("CUSTOMER")
                        // Deny by default: a new endpoint is closed until someone opens it on purpose.
                        .anyRequest().denyAll())
                // Username and password in the Authorization header of every request.
                .httpBasic(Customizer.withDefaults())
                // No HTTP session: every request carries its own credentials.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // CSRF tricks a browser into sending credentials it holds to this API from another
                // site. Careful with HTTP Basic: once a person types the password into the browser's
                // own login prompt, the browser adds the Authorization header by itself. Here only
                // curl and tests use Basic, and lesson 15 moves to bearer tokens, which a browser
                // never adds on its own. Turn CSRF back on for cookies or for Basic in a browser.
                .csrf(csrf -> csrf.disable())
                // Uses the CorsConfigurationSource bean below.
                .cors(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(corsProperties.allowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Idempotency-Key", "API-Version"));
        config.setExposedHeaders(List.of("Location", "Idempotent-Replayed"));
        // Browsers may cache the preflight answer for an hour.
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

}
