package com.shoplite;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ApiDocsTests extends IntegrationTestSupport {

    @Test
    void version1DocumentDescribesTheVersion1OrderResponse() {
        assertThat(mvc.get().uri("/v3/api-docs/v1"))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.info.title", title -> assertThat(title).isEqualTo("ShopLite Orders API"))
                .hasPathSatisfying("$.info.version", version -> assertThat(version).isEqualTo("1.0"))
                .hasPathSatisfying("$.paths['/api/orders/{id}'].get.responses['200'].content['*/*'].schema['$ref']",
                        ref -> assertThat(ref).asString().endsWith("/OrderResponse"));
    }

    @Test
    void version2DocumentDescribesTheVersion2OrderResponse() {
        assertThat(mvc.get().uri("/v3/api-docs/v2"))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.paths['/api/orders/{id}'].get.responses['200'].content['*/*'].schema['$ref']",
                        ref -> assertThat(ref).asString().endsWith("/OrderResponseV2"));
    }

    @Test
    void swaggerUiIsPublic() {
        assertThat(mvc.get().uri("/swagger-ui/index.html")).hasStatusOk();
    }

}
