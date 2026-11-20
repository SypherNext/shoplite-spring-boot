package com.shoplite.common;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

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
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }

    private OpenApiDefaults() {
    }

}
