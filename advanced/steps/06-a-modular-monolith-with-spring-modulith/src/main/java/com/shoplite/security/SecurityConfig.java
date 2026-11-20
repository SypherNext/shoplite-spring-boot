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
 * Tokens are checked by the OAuth2 resource server support: signature (with Keycloak's public keys),
 * issuer, audience and expiry. Roles come from the token's "roles" claim (see application.properties).
 */
@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity // turns on @PreAuthorize
@EnableConfigurationProperties(CorsProperties.class)
public class SecurityConfig {

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // Health checks for load balancers, and the version info.
                        .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        // Spring Boot's error page: without this, every error would turn into a 403.
                        .requestMatchers("/error").permitAll()
                        // Swagger UI and the OpenAPI JSON. In production they are switched off
                        // (application-prod.properties), so these paths answer 404 there.
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // Anyone may browse the catalog.
                        .requestMatchers(HttpMethod.GET, "/api/products/**", "/api/categories/**", "/api/shop")
                        .permitAll()
                        // Changing the catalog, admin pages and the other actuator endpoints need ADMIN.
                        .requestMatchers("/api/products/**", "/api/categories/**", "/api/admin/**", "/actuator/**")
                        .hasRole("ADMIN")
                        .requestMatchers("/api/orders/**", "/api/customers/**").hasRole("CUSTOMER")
                        // Deny by default: a new endpoint is closed until someone opens it on purpose.
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                // No HTTP session: every request carries its own token.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // CSRF tricks a browser into sending its cookies to another site. This API reads
                // credentials only from the Authorization header, which a browser never adds by
                // itself, so CSRF protection has nothing to protect. Turn it back on if you ever
                // switch to cookie-based sessions.
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
