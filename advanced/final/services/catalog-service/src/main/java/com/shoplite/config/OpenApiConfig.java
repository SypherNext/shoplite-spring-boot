package com.shoplite.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.shoplite.common.OpenApiDefaults;

/** The catalog's OpenAPI document. /internal/** is for other services and is left out. */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    @Bean
    OpenAPI catalogOpenApi() {
        return OpenApiDefaults.withBearerJwt("ShopLite Catalog API", "1.0",
                "Categories and products. Reading is public; changing the catalog needs an ADMIN token.");
    }

    @Bean
    GroupedOpenApi publicCatalogApi() {
        return GroupedOpenApi.builder().group("catalog").pathsToMatch("/api/**").build();
    }

}
