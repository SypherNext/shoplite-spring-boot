package com.shoplite.assistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.shoplite.TestcontainersConfiguration;
import com.shoplite.assistant.PolicyTools.PolicyPassage;

/** The policy texts in pgvector: loaded once at startup, and found again by the policy tool. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class PolicyRetrievalTests {

    @MockitoBean
    ChatModel chatModel;

    @Autowired
    AssistantService assistant;

    @Autowired
    PolicyLibrary policies;

    @Autowired
    PolicyTools policyTools;

    @Autowired
    JdbcClient jdbc;

    final List<Prompt> prompts = new ArrayList<>();

    @BeforeEach
    void modelRecordsPrompts() {
        prompts.clear();
        given(chatModel.getOptions()).willReturn(ToolCallingChatOptions.builder().build());
        given(chatModel.call(any(Prompt.class))).willAnswer(call -> {
            prompts.add(call.getArgument(0));
            return new ChatResponse(List.of(new Generation(new AssistantMessage("ok"))));
        });
    }

    @Test
    void everyPolicyFileIsEmbeddedAtStartup() {
        assertThat(jdbc.sql("select source from policy_sources order by source").query(String.class).list())
                .containsExactly("payments.md", "returns.md", "shipping.md", "warranty.md");
        assertThat(count("select count(*) from vector_store")).isPositive();
    }

    @Test
    void anUnchangedFileIsNotEmbeddedAgain() {
        int before = count("select count(*) from vector_store");
        String text = jdbc.sql("select content from vector_store where metadata->>'source' = 'returns.md' limit 1")
                .query(String.class).single();

        policies.load("returns.md", "# Returns and refunds\n\nA different text: this file changed.");
        int changed = count("select count(*) from vector_store where metadata->>'source' = 'returns.md'");
        policies.load("returns.md", "# Returns and refunds\n\nA different text: this file changed.");

        assertThat(count("select count(*) from vector_store where metadata->>'source' = 'returns.md'")).isEqualTo(changed);
        assertThat(text).contains("30 days");
        assertThat(before).isPositive();
    }

    @Test
    void policySearchFindsTheMatchingPolicy() {
        assertThat(policyTools.searchShopPolicies("Can I send a mug back and get a refund?"))
                .extracting(PolicyPassage::policy).containsExactly("returns.md");
        assertThat(policyTools.searchShopPolicies("Can I send a mug back and get a refund?").getFirst().text())
                .contains("within 30 days of delivery");
    }

    @Test
    void anUnrelatedQuestionFindsNoPolicy() {
        assertThat(policyTools.searchShopPolicies("Which mugs do you sell?")).isEmpty();
    }

    @Test
    void theModelIsOfferedThePolicyTool() {
        assistant.ask("alice", null, "Do you ship to Norway?");

        ToolCallingChatOptions options = (ToolCallingChatOptions) prompts.getFirst().getOptions();
        assertThat(options.getToolCallbacks()).extracting(tool -> tool.getToolDefinition().name())
                .containsExactlyInAnyOrder("searchProducts", "productDetails", "searchShopPolicies");
    }
    private int count(String sql) {
        return jdbc.sql(sql).query(Integer.class).single();
    }

}
