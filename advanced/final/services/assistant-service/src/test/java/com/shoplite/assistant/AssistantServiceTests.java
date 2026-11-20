package com.shoplite.assistant;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.web.client.ResourceAccessException;

import com.shoplite.TestJwts;
import com.shoplite.TestcontainersConfiguration;
import com.shoplite.assistant.CatalogClient.ProductHit;

/**
 * The assistant without a real model: the ChatModel is a Mockito mock that records every prompt,
 * and WireMock plays the catalog. What the real model answers is shown in the lesson, not tested:
 * it varies from run to run.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AssistantServiceTests {

    static final WireMockServer catalog = new WireMockServer(options().dynamicPort().http2PlainDisabled(true));

    static {
        catalog.start();
    }

    @DynamicPropertySource
    static void fakeCatalog(DynamicPropertyRegistry registry) {
        registry.add("spring.http.serviceclient.catalog.base-url", catalog::baseUrl);
    }

    @AfterAll
    static void stopCatalog() {
        catalog.stop();
    }

    @MockitoBean
    ChatModel chatModel;

    @Autowired
    MockMvcTester mvc;

    @Autowired
    CatalogTools tools;

    final List<Prompt> prompts = new ArrayList<>();

    @BeforeEach
    void modelAnswersWithACountingReply() {
        catalog.resetAll();
        prompts.clear();
        // ChatClient starts from the model's default options and adds the tools to them.
        given(chatModel.getOptions()).willReturn(ToolCallingChatOptions.builder().build());
        given(chatModel.call(any(Prompt.class))).willAnswer(call -> {
            prompts.add(call.getArgument(0));
            return new ChatResponse(List.of(new Generation(new AssistantMessage("Answer " + prompts.size()))));
        });
    }

    @Test
    void searchToolReturnsTheCatalogsMatches() {
        catalog.stubFor(get(urlPathEqualTo("/api/products")).willReturn(okJson("""
                {"content": [{"id": 3, "name": "Ceramic Mug", "sku": "MUG-1", "price": 12.50,
                              "stock": 40, "categoryName": "Kitchen"}],
                 "page": {"size": 5, "number": 0, "totalElements": 1, "totalPages": 1}}
                """)));

        assertThat(tools.searchProducts("mug"))
                .containsExactly(new ProductHit(3, "Ceramic Mug", new BigDecimal("12.50"), 40, "Kitchen"));
        assertThat(catalog.getAllServeEvents().getFirst().getRequest().getUrl())
                .isEqualTo("/api/products?search=mug&size=5");
    }

    @Test
    void detailsToolReturnsNullForAnUnknownProduct() {
        catalog.stubFor(get(urlPathEqualTo("/api/products/999")).willReturn(aResponse().withStatus(404)));

        assertThat(tools.productDetails(999)).isNull();
    }

    @Test
    void conversationRemembersEarlierMessagesForTheSameCustomerOnly() throws Exception {
        String conversationId = JsonPath.read(chat(TestJwts.customer("alice"), null, "Do you sell mugs?")
                .getResponse().getContentAsString(), "$.conversationId");

        assertThat(chat(TestJwts.customer("alice"), conversationId, "How much is the cheapest?"))
                .bodyJson().extractingPath("$.answer").isEqualTo("Answer 2");
        // The second prompt carries the first exchange from memory.
        assertThat(texts(prompts.get(1))).containsSubsequence("Do you sell mugs?", "Answer 1");
        assertThat(prompts.get(1).getUserMessage().getText()).startsWith("How much is the cheapest?");

        // Bob uses alice's conversation id: his prompt starts empty, nothing of alice's leaks.
        chat(TestJwts.customer("bob"), conversationId, "What did I ask before?");
        assertThat(texts(prompts.get(2))).doesNotContain("Do you sell mugs?");
        assertThat(prompts.get(2).getUserMessage().getText()).startsWith("What did I ask before?");
    }

    @Test
    void systemPromptKeepsTheModelOnShopLiteProducts() {
        chat(TestJwts.customer("alice"), null, "Hello");

        assertThat(prompts.getFirst().getInstructions().getFirst().getText())
                .startsWith("You are the shopping assistant of ShopLite")
                .contains("Never invent products, prices or stock levels");
    }

    @Test
    void onlySignedInCustomersMayChat() {
        assertThat(mvc.post().uri("/api/assistant/chat").contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\": \"Hi\"}")).hasStatus(401);
        assertThat(chat(TestJwts.admin(), null, "Hi")).hasStatus(403);
    }

    @Test
    void blankOrHugeMessagesAreRejected() {
        assertThat(chat(TestJwts.customer("alice"), null, " ")).hasStatus(400);
        assertThat(chat(TestJwts.customer("alice"), null, "a".repeat(1001))).hasStatus(400);
        assertThat(prompts).isEmpty();
    }

    @Test
    void unreachableModelIsA503() {
        given(chatModel.call(any(Prompt.class))).willThrow(new ResourceAccessException("Connection refused"));

        assertThat(chat(TestJwts.customer("alice"), null, "Hi")).hasStatus(503)
                .bodyJson().extractingPath("$.detail").isEqualTo("The assistant's language model is not reachable");
    }

    private MvcTestResult chat(org.springframework.test.web.servlet.request.RequestPostProcessor user,
            String conversationId, String message) {
        String id = conversationId == null ? "null" : "\"" + conversationId + "\"";
        return mvc.post().uri("/api/assistant/chat").with(user).contentType(MediaType.APPLICATION_JSON)
                .content("{\"conversationId\": " + id + ", \"message\": \"" + message + "\"}").exchange();
    }

    private static List<String> texts(Prompt prompt) {
        return prompt.getInstructions().stream().map(Message::getText).toList();
    }

}