package com.shoplite;

import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

/**
 * A stand-in for nomic-embed-text in tests: each policy topic is one dimension, and its value is how often
 * the topic's words occur in the text. Texts about the same topic point the same way, so the vector store's
 * similarity search behaves like the real thing, but the results never change and no model is needed.
 */
class TopicEmbeddingModel implements EmbeddingModel {

    private static final int DIMENSIONS = 768;
    private static final List<List<String>> TOPICS = List.of(
            List.of("return", "refund"),
            List.of("ship", "deliver", "parcel"),
            List.of("warranty", "defect"),
            List.of("pay", "card", "invoice"));

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<String> texts = request.getInstructions();
        return new EmbeddingResponse(IntStream.range(0, texts.size())
                .mapToObj(i -> new Embedding(vector(texts.get(i)), i))
                .toList());
    }

    @Override
    public float[] embed(Document document) {
        return vector(document.getText());
    }

    @Override
    public int dimensions() {
        return DIMENSIONS;
    }

    private static float[] vector(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        float[] vector = new float[DIMENSIONS];
        for (int topic = 0; topic < TOPICS.size(); topic++) {
            vector[topic] = TOPICS.get(topic).stream().mapToInt(word -> occurrences(lower, word)).sum();
        }
        // A small constant part, so a text without any topic word is not a zero vector.
        vector[DIMENSIONS - 1] = 0.1f;
        return vector;
    }

    private static int occurrences(String text, String word) {
        int count = 0;
        for (int at = text.indexOf(word); at >= 0; at = text.indexOf(word, at + word.length())) {
            count++;
        }
        return count;
    }

}
