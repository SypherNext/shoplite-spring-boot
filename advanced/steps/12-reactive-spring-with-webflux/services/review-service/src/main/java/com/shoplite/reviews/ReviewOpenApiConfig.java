package com.shoplite.reviews;

import java.util.List;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** The review service's OpenAPI document, shown in its own Swagger UI and in the gateway's. */
@Configuration(proxyBeanMethods = false)
class ReviewOpenApiConfig {

    @Bean
    OpenAPI reviewsOpenApi() {
        return new OpenAPI()
                .info(new Info().title("ShopLite Reviews API").version("1.0")
                        .description("Product reviews. Reading is public; writing a review needs a CUSTOMER token."))
                .components(new Components().addSecuritySchemes("bearer-jwt",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"))
                .servers(List.of(new Server().url("/")));
    }

    @Bean
    GroupedOpenApi reviewsApi() {
        return GroupedOpenApi.builder().group("reviews").pathsToMatch("/api/**").build();
    }

}
