package com.shoplite;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ApiDocsTests {

    @Autowired
    MockMvcTester mvc;

    @Test
    void version1DocumentDescribesTheVersion1OrderResponse() {
        assertThat(mvc.get().uri("/v3/api-docs/v1"))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.info.version", version -> assertThat(version).isEqualTo("1.0"))
                .hasPathSatisfying("$.paths['/api/orders/{id}'].get.responses['200'].content['*/*'].schema['$ref']",
                        ref -> assertThat(ref).asString().endsWith("/OrderResponse"))
                .hasPathSatisfying("$.components.securitySchemes['bearer-jwt'].scheme",
                        scheme -> assertThat(scheme).asString().isEqualTo("bearer"));
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
    void swaggerUiIsServed() {
        assertThat(mvc.get().uri("/swagger-ui/index.html")).hasStatusOk();
    }

}
