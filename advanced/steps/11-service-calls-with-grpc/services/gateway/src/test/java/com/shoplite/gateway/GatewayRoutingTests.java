package com.shoplite.gateway;

import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;

/**
 * The real gateway on a random port, with two WireMock servers standing in for the catalog
 * and order services. Each test sends a request to the gateway and checks where it arrived.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRoutingTests {

    static final WireMockServer catalog = new WireMockServer(options().dynamicPort().http2PlainDisabled(true));
    static final WireMockServer orders = new WireMockServer(options().dynamicPort().http2PlainDisabled(true));

    static {
        catalog.start();
        orders.start();
    }

    @DynamicPropertySource
    static void services(DynamicPropertyRegistry registry) {
        registry.add("shoplite.services.catalog", catalog::baseUrl);
        registry.add("shoplite.services.orders", orders::baseUrl);
    }

    @LocalServerPort
    int port;

    RestClient client;

    @BeforeEach
    void setUp() {
        catalog.resetAll();
        orders.resetAll();
        client = RestClient.builder().baseUrl("http://localhost:" + port)
                .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> { })
                .build();
    }

    @Test
    void productRequestsGoToTheCatalog() {
        catalog.stubFor(get(urlPathEqualTo("/api/products")).willReturn(okJson("{\"content\": []}")));

        ResponseEntity<String> response = client.get().uri("/api/products?size=2").retrieve().toEntity(String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        catalog.verify(getRequestedFor(urlEqualTo("/api/products?size=2")));
        orders.verify(0, anyRequestedFor(anyUrl()));
    }

    @Test
    void orderRequestsGoToTheOrderServiceWithTheTokenUnchanged() {
        orders.stubFor(get(urlPathEqualTo("/api/orders")).willReturn(okJson("{\"content\": []}")));

        client.get().uri("/api/orders").header("Authorization", "Bearer test-token").retrieve().toBodilessEntity();

        orders.verify(getRequestedFor(urlPathEqualTo("/api/orders"))
                .withHeader("Authorization", equalTo("Bearer test-token")));
    }

    @Test
    void docsRoutesStripTheirPrefix() {
        catalog.stubFor(get(urlPathEqualTo("/v3/api-docs/catalog")).willReturn(okJson("{\"openapi\": \"3.1.0\"}")));

        assertThat(client.get().uri("/docs/catalog/v3/api-docs/catalog").retrieve().toEntity(String.class).getBody())
                .contains("3.1.0");
    }

    @Test
    void internalEndpointsAreNotRouted() {
        ResponseEntity<String> response = client.post().uri("/internal/stock/reservations")
                .retrieve().toEntity(String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        catalog.verify(0, anyRequestedFor(anyUrl()));
    }

    @Test
    void swaggerUiListsEveryServiceDocument() {
        assertThat(client.get().uri("/v3/api-docs/swagger-config").retrieve().toEntity(String.class).getBody())
                .contains("/docs/catalog/v3/api-docs/catalog", "/docs/orders/v3/api-docs/v1",
                        "/docs/orders/v3/api-docs/v2");
    }

}
