package com.shoplite.security;

import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

/**
 * The security rules every ShopLite API shares. Each service adds its own URL rules on top.
 */
public final class ApiSecurity {

    /** Health checks, version info, Spring Boot's error page and Swagger UI stay open in every service. */
    public static final String[] PUBLIC_PATHS = {
            "/actuator/health/**", "/actuator/info", "/error", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**"};

    /**
     * JWT bearer tokens checked by the OAuth2 resource server support, no HTTP session, no CSRF
     * protection (no cookies are used, so there is nothing for CSRF to steal) and CORS from
     * CorsProperties.
     */
    public static HttpSecurity statelessJwtApi(HttpSecurity http) throws Exception {
        return http
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults());
    }

    private ApiSecurity() {
    }

}
