package com.example.smartagent.service.provider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SpringAIEmbeddingProvider implements EmbeddingProvider {

    private final EmbeddingModel embeddingModel;

    @Override
    public String getProviderName() {
        return "springai-openai";
    }

    @Override
    public List<Float> generateEmbedding(String text) {
        try {
            EmbeddingResponse response = embeddingModel.embedForResponse(List.of(text));
            if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
                log.warn("Empty response from Spring AI embedding API");
                return List.of();
            }
            
            float[] embedding = response.getResults().get(0).getOutput();
            if (embedding == null) {
                log.warn("No embedding array in response");
                return List.of();
            }
            
            List<Float> result = new java.util.ArrayList<>();
            for (float f : embedding) {
                result.add(f);
            }
            return result;

        } catch (Exception e) {
            log.error("Spring AI embedding request failed", e);
            return List.of();
        }
    }

    @Override
    public int getDimension() {
        try {
            return embeddingModel.dimensions();
        } catch (Exception e) {
            log.warn("Failed to get embedding dimension, using default 1536", e);
            return 1536;
        }
    }
}
