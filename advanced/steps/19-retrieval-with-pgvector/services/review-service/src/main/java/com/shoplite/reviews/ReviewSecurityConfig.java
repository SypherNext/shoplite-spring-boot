package com.shoplite.reviews;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/** The reactive twin of the other services' SecurityFilterChain: same rules, WebFlux types. */
@Configuration(proxyBeanMethods = false)
class ReviewSecurityConfig {

    @Bean
    SecurityWebFilterChain reviewSecurity(ServerHttpSecurity http) {
        return http
                .authorizeExchange(exchange -> exchange
                        .pathMatchers("/actuator/health/**", "/actuator/info", "/swagger-ui.html", "/swagger-ui/**",
                                "/webjars/**", "/v3/api-docs/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/products/*/reviews/**", "/api/reviews/stream").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/products/*/reviews").hasRole("CUSTOMER")
                        .anyExchange().denyAll())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .build();
    }

}
