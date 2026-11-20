package com.shoplite.common;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The title block shown at the top of Swagger UI (/swagger-ui.html)
 * and in the raw OpenAPI document (/v3/api-docs).
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    @Bean
    OpenAPI shopliteOpenApi() {
        return new OpenAPI().info(new Info()
                .title("ShopLite API")
                .version("1.0")
                .description("Catalog API for ShopLite, a small online shop: categories and products."));
    }

}
