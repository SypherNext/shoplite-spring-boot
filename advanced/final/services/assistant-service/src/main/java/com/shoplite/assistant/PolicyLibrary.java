package com.shoplite.assistant;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Loads the shop's policy texts (src/main/resources/policies) into the vector store at startup:
 * split into chunks, embedded by the embedding model, stored in PostgreSQL. A file whose content did not
 * change since the last run is skipped, so restarts do not pay for embeddings again.
 */
@Component
public class PolicyLibrary implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PolicyLibrary.class);

    private final VectorStore vectorStore;
    private final JdbcClient jdbc;
    private final ResourcePatternResolver resources;
    // About 200 tokens per chunk: a few sentences, small enough to stay on one topic.
    private final TokenTextSplitter splitter = TokenTextSplitter.builder().withChunkSize(200).build();

    public PolicyLibrary(VectorStore vectorStore, JdbcClient jdbc, ResourcePatternResolver resources) {
        this.vectorStore = vectorStore;
        this.jdbc = jdbc;
        this.resources = resources;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        for (Resource file : resources.getResources("classpath:policies/*.md")) {
            load(file.getFilename(), file.getContentAsString(StandardCharsets.UTF_8));
        }
    }

    void load(String source, String text) {
        String hash = sha256(text);
        boolean unchanged = jdbc.sql("select count(*) from policy_sources where source = ? and content_hash = ?")
                .params(source, hash).query(Integer.class).single() > 0;
        if (unchanged) {
            log.info("Policy {} unchanged, keeping its embeddings", source);
            return;
        }
        // Replace the old chunks of this file, if any, with the new ones.
        vectorStore.delete(new FilterExpressionBuilder().eq("source", source).build());
        List<Document> chunks = splitter.apply(List.of(new Document(text, Map.of("source", source))));
        vectorStore.add(chunks);
        jdbc.sql("""
                insert into policy_sources (source, content_hash, chunks) values (?, ?, ?)
                on conflict (source) do update set content_hash = excluded.content_hash,
                    chunks = excluded.chunks, embedded_at = now()
                """).params(source, hash, chunks.size()).update();
        log.info("Policy {} embedded: {} chunks", source, chunks.size());
    }

    private static String sha256(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        }
        catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

}
