package com.shoplite.common;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * OpenAPI document and Swagger UI setup.
 * One document per API version: Swagger UI shows a "Select a definition" box with 1.0 and 2.0.
 * The "bearer-jwt" scheme makes the Authorize button accept a Keycloak access token.
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearer-jwt";

    private static final String DESCRIPTION = """
            Catalog and orders API for ShopLite, a small online shop.
            Send the API-Version header (1.0 or 2.0) to pick a version; no header means 1.0.
            For now, orders name their customer in the Customer-Email header. Once sign-in
            is switched on, click Authorize and paste an access token.""";

    @Bean
    OpenAPI shopliteOpenApi() {
        return new OpenAPI()
                .components(new Components().addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                // Applies to every operation. Public endpoints simply ignore the token.
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }

    @Bean
    GroupedOpenApi version1Docs() {
        return apiVersionGroup("v1", "1.0");
    }

    @Bean
    GroupedOpenApi version2Docs() {
        return apiVersionGroup("v2", "2.0");
    }

    /**
     * A document with every endpoint that has no version (it serves all versions)
     * plus the endpoints mapped to exactly this version.
     */
    private static GroupedOpenApi apiVersionGroup(String group, String version) {
        return GroupedOpenApi.builder()
                .group(group)
                .displayName("ShopLite API " + version)
                .pathsToMatch("/api/**")
                .addOpenApiMethodFilter(method -> {
                    RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
                    String mappedVersion = mapping == null ? "" : mapping.version();
                    return mappedVersion.isEmpty() || mappedVersion.equals(version);
                })
                .addOpenApiCustomizer(openApi -> openApi.info(new Info()
                        .title("ShopLite API")
                        .version(version)
                        .description(DESCRIPTION)))
                .build();
    }

}
