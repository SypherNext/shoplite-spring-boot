package com.shoplite.common;

import java.util.List;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

/** The OpenAPI document header every service shares: a title and the "bearer-jwt" Authorize button. */
public final class OpenApiDefaults {

    public static final String BEARER_SCHEME = "bearer-jwt";

    public static OpenAPI withBearerJwt(String title, String version, String description) {
        return new OpenAPI()
                .info(new Info().title(title).version(version).description(description))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                // A relative server URL: Swagger UI calls the API on whatever host served the document,
                // the service itself (:8281) or the gateway (:8280).
                .servers(List.of(new Server().url("/")));
    }

    private OpenApiDefaults() {
    }

}
