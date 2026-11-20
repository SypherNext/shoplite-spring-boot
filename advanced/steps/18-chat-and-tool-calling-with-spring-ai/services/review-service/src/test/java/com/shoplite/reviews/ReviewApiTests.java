package com.shoplite.reviews;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

import java.time.Duration;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

/**
 * The review service with PostgreSQL from Testcontainers (one container gives
 * both the R2DBC and the JDBC connection, for Flyway), and WireMock as the catalog.
 */
// MOCK environment (the default): WebTestClient talks to the application without a real port,
// which is what mockJwt() needs to put a test token into the request.
@SpringBootTest
@AutoConfigureWebTestClient
@Import(ReviewApiTests.Containers.class)
class ReviewApiTests {

    @TestConfiguration(proxyBeanMethods = false)
    static class Containers {

        @Bean
        @ServiceConnection
        PostgreSQLContainer postgres() {
            return new PostgreSQLContainer(DockerImageName.parse("postgres:18.7-alpine"));
        }

        // WireMock as a bean, not a static field: it starts with the test context and stops with it.
        // A static server would also start when the native build processes the tests ahead of time,
        // and its threads would keep that build step from ever finishing.
        @Bean(initMethod = "start", destroyMethod = "stop")
        WireMockServer catalog() {
            return new WireMockServer(options().dynamicPort().http2PlainDisabled(true));
        }

        /** Spring Cloud's simple discovery client: catalog-service is "found" at the WireMock address. */
        @Bean
        DynamicPropertyRegistrar catalogInstance(WireMockServer catalog) {
            return registry -> registry.add("spring.cloud.discovery.client.simple.instances.catalog-service[0].uri",
                    catalog::baseUrl);
        }

    }

    @Autowired
    WireMockServer catalog;

    @Autowired
    WebTestClient web;

    @BeforeEach
    void stubCatalog() {
        catalog.resetAll();
        catalog.stubFor(get(urlPathEqualTo("/api/products/1")).willReturn(okJson("{\"id\": 1}")));
        catalog.stubFor(get(urlPathEqualTo("/api/products/999")).willReturn(aResponse().withStatus(404)));
    }

    @Test
    void aCustomerReviewsAProductAndEveryoneCanReadIt() {
        post(1, "alice", 5, "Keeps coffee hot on cold mornings").expectStatus().isCreated()
                .expectBody().jsonPath("$.authorName").isEqualTo("Alice");

        web.get().uri("/api/products/1/reviews").exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$[0].rating").isEqualTo(5);
        web.get().uri("/api/products/1/reviews/summary").exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.reviews").isEqualTo(1);
    }

    @Test
    void oneReviewPerCustomerAndProduct() {
        post(1, "erin", 4, "Good").expectStatus().isCreated();

        post(1, "erin", 2, "Changed my mind").expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.detail").isEqualTo("You have already reviewed this product");
    }

    @Test
    void aProductTheCatalogDoesNotKnowCannotBeReviewed() {
        post(999, "bob", 4, "Nice").expectStatus().isNotFound()
                .expectBody().jsonPath("$.detail").isEqualTo("Product 999 was not found");
    }

    @Test
    void aRatingOutsideOneToFiveIsRejected() {
        post(1, "carol", 7, "Too good").expectStatus().isBadRequest();
    }

    @Test
    void writingNeedsAToken() {
        web.post().uri("/api/products/1/reviews").contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"rating\": 4}").exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void newReviewsArePushedToTheStream() {
        Flux<ServerSentEvent<ReviewResponse>> stream = web.get().uri("/api/reviews/stream")
                .accept(MediaType.TEXT_EVENT_STREAM).exchange()
                .expectStatus().isOk()
                .returnResult(new ParameterizedTypeReference<ServerSentEvent<ReviewResponse>>() { })
                .getResponseBody();

        StepVerifier.create(stream.filter(event -> "review".equals(event.event())))
                .expectSubscription()
                // The sink only sends to subscribers that are already there: give the server a moment
                // to subscribe to it before the review is posted.
                .thenAwait(Duration.ofSeconds(1))
                .then(() -> post(1, "dave", 3, "Fine for the price").expectStatus().isCreated())
                .assertNext(event -> assertThat(event.data().authorName()).isEqualTo("Dave"))
                .thenCancel()
                .verify(Duration.ofSeconds(10));
    }

    private WebTestClient.ResponseSpec post(long productId, String user, int rating, String comment) {
        String name = Character.toUpperCase(user.charAt(0)) + user.substring(1);
        return web.mutateWith(mockJwt()
                        .jwt(jwt -> jwt.subject("subject-" + user).claim("name", name))
                        .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                .post().uri("/api/products/{id}/reviews", productId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"rating\": %d, \"comment\": \"%s\"}".formatted(rating, comment))
                .exchange();
    }

}