package com.shoplite.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import com.shoplite.common.OpenApiDefaults;

/**
 * The order service's OpenAPI documents: one per API version, so Swagger UI shows a
 * "Select a definition" box with 1.0 and 2.0.
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    private static final String DESCRIPTION = """
            Customers and orders. Send the API-Version header (1.0 or 2.0); no header means 1.0.
            Everything needs a Keycloak access token: click Authorize and paste the token.""";

    @Bean
    OpenAPI ordersOpenApi() {
        return OpenApiDefaults.withBearerJwt("ShopLite Orders API", "1.0", DESCRIPTION);
    }

    @Bean
    GroupedOpenApi version1Docs() {
        return apiVersionGroup("v1", "1.0");
    }

    @Bean
    GroupedOpenApi version2Docs() {
        return apiVersionGroup("v2", "2.0");
    }

    /** Every endpoint without a version (it serves all versions) plus the ones mapped to exactly this version. */
    private static GroupedOpenApi apiVersionGroup(String group, String version) {
        return GroupedOpenApi.builder()
                .group(group)
                .displayName("ShopLite Orders API " + version)
                .pathsToMatch("/api/**")
                .addOpenApiMethodFilter(method -> {
                    RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
                    String mappedVersion = mapping == null ? "" : mapping.version();
                    return mappedVersion.isEmpty() || mappedVersion.equals(version);
                })
                .addOpenApiCustomizer(openApi -> openApi.info(new Info()
                        .title("ShopLite Orders API").version(version).description(DESCRIPTION)))
                .build();
    }

}
