package com.shoplite;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ApiDocsTests extends IntegrationTestSupport {

    @Test
    void theCatalogDocumentLeavesOutTheInternalApi() {
        assertThat(mvc.get().uri("/v3/api-docs/catalog"))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.info.title", title -> assertThat(title).isEqualTo("ShopLite Catalog API"))
                .hasPathSatisfying("$.paths", paths -> assertThat(paths).asMap()
                        .containsKey("/api/products/{id}")
                        .doesNotContainKeys("/internal/stock/reservations", "/internal/stock/releases"));
    }

    @Test
    void swaggerUiIsPublic() {
        assertThat(mvc.get().uri("/swagger-ui/index.html")).hasStatusOk();
    }

}
