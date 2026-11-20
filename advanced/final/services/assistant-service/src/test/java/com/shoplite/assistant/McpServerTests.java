package com.shoplite.assistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.shoplite.TestcontainersConfiguration;

/** A real MCP client from the MCP Java SDK, talking Streamable HTTP to the running service. */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class McpServerTests {

    @LocalServerPort
    int port;

    @MockitoBean
    ChatModel chatModel;

    /** "customer-token" is a valid CUSTOMER token for this test; no Keycloak needed. */
    @MockitoBean
    JwtDecoder jwtDecoder;

    @BeforeEach
    void customerToken() {
        given(jwtDecoder.decode("customer-token")).willReturn(Jwt.withTokenValue("customer-token")
                .header("alg", "none")
                .subject("subject-alice")
                .claim("preferred_username", "alice")
                .claim("roles", List.of("CUSTOMER"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build());
    }

    @Test
    void listsTheShopLiteTools() {
        try (McpSyncClient client = client("customer-token")) {
            client.initialize();

            assertThat(client.listTools().tools()).extracting(Tool::name)
                    .containsExactlyInAnyOrder("searchProducts", "productDetails", "searchShopPolicies");
        }
    }

    @Test
    void policySearchAnswersFromThePolicyTexts() {
        try (McpSyncClient client = client("customer-token")) {
            client.initialize();

            CallToolResult result = client.callTool(new CallToolRequest("searchShopPolicies",
                    Map.of("query", "How do I get a refund for a return?")));

            assertThat(result.isError()).isFalse();
            assertThat(((TextContent) result.content().getFirst()).text())
                    .contains("returns.md").contains("within 30 days of delivery");
        }
    }

    @Test
    void noTokenNoTools() throws Exception {
        try (McpSyncClient client = client(null)) {
            assertThatThrownBy(client::initialize).isInstanceOf(RuntimeException.class);
        }
        // The reason, seen without the SDK: the endpoint answers 401 before any MCP message is read.
        HttpResponse<String> response = HttpClient.newHttpClient().send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/mcp"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{}")).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(401);
    }

    private McpSyncClient client(String token) {
        HttpRequest.Builder request = HttpRequest.newBuilder();
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        HttpClientStreamableHttpTransport transport = HttpClientStreamableHttpTransport
                .builder("http://localhost:" + port)
                .requestBuilder(request)
                .build();
        return McpClient.sync(transport).build();
    }

}