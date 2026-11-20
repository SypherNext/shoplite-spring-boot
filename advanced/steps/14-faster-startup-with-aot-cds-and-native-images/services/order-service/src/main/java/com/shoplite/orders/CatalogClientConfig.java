package com.shoplite.orders;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;
import org.springframework.web.service.registry.ImportHttpServices;

/**
 * Registers CatalogStockClient in the "catalog" group and adds token relay to that group:
 * every call carries the access token of the customer whose request is being handled,
 * so the catalog can check it exactly like a token that came from a browser.
 */
@Configuration(proxyBeanMethods = false)
@ImportHttpServices(group = "catalog", types = CatalogStockClient.class)
class CatalogClientConfig {

    @Bean
    RestClientHttpServiceGroupConfigurer catalogTokenRelay() {
        return groups -> groups.filterByName("catalog")
                .forEachClient((group, builder) -> builder.requestInterceptor(relayBearerToken()));
    }

    private static ClientHttpRequestInterceptor relayBearerToken() {
        return (request, body, execution) -> {
            if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken jwt) {
                request.getHeaders().setBearerAuth(jwt.getToken().getTokenValue());
            }
            return execution.execute(request, body);
        };
    }

}
