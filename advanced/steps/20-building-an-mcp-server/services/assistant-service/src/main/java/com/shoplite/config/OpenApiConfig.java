package com.shoplite.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.shoplite.common.OpenApiDefaults;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    @Bean
    OpenAPI assistantOpenApi() {
        return OpenApiDefaults.withBearerJwt("ShopLite Assistant API", "1.0",
                "A shopping assistant backed by a local language model. Needs a CUSTOMER token.");
    }

}
