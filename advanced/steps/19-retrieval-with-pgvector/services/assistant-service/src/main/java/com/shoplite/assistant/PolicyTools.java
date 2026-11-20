package com.shoplite.assistant;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

/**
 * Retrieval as a tool: the model decides when a question needs the shop's policies and what to search for.
 * The similarity threshold keeps unrelated passages out: below 0.5, a passage is not about the question.
 */
@Component
public class PolicyTools {

    private static final Logger log = LoggerFactory.getLogger(PolicyTools.class);

    private final VectorStore vectorStore;

    public PolicyTools(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Tool(description = "Search ShopLite's shop policies (returns, shipping, warranty, payments). "
            + "Returns up to 3 matching passages with the policy file they come from.")
    public List<PolicyPassage> searchShopPolicies(
            @ToolParam(description = "The question or topic, for example 'how long do I have to return an item'") String query) {
        log.info("Tool call: searchShopPolicies(\"{}\")", query);
        List<Document> matches = vectorStore.similaritySearch(
                SearchRequest.builder().query(query).topK(3).similarityThreshold(0.5).build());
        return matches.stream()
                .map(doc -> new PolicyPassage(String.valueOf(doc.getMetadata().get("source")), doc.getText()))
                .toList();
    }

    public record PolicyPassage(String policy, String text) {
    }

}
