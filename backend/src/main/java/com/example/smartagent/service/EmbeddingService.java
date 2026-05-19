package com.example.smartagent.service;

import com.example.smartagent.service.provider.EmbeddingProvider;
import com.example.smartagent.service.provider.ProviderFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingService {

    private final ProviderFactory providerFactory;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<Float> generateEmbedding(String text) {
        EmbeddingProvider provider = providerFactory.getDefaultEmbeddingProvider();
        if (provider == null) {
            log.error("No embedding provider available");
            return List.of();
        }
        return provider.generateEmbedding(text);
    }

    public List<Float> generateEmbedding(String text, String providerName) {
        EmbeddingProvider provider = providerFactory.getEmbeddingProvider(providerName);
        if (provider == null) {
            log.warn("Provider not found: {}, using default", providerName);
            return generateEmbedding(text);
        }
        return provider.generateEmbedding(text);
    }

    public double calculateSimilarity(List<Float> vec1, List<Float> vec2) {
        if (vec1.isEmpty() || vec2.isEmpty()) return 0;
        return cosineSimilarity(vec1, vec2);
    }

    private double cosineSimilarity(List<Float> vec1, List<Float> vec2) {
        double dotProduct = 0, norm1 = 0, norm2 = 0;
        int minSize = Math.min(vec1.size(), vec2.size());

        for (int i = 0; i < minSize; i++) {
            dotProduct += vec1.get(i) * vec2.get(i);
            norm1 += Math.pow(vec1.get(i), 2);
            norm2 += Math.pow(vec2.get(i), 2);
        }

        if (norm1 == 0 || norm2 == 0) return 0;
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    public List<Float> parseVectorJson(String vectorJson) {
        if (vectorJson == null || vectorJson.isEmpty()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(vectorJson, new TypeReference<List<Float>>() {});
        } catch (JsonProcessingException e) {
            log.error("Failed to parse vector JSON", e);
            return List.of();
        }
    }

    public String vectorToJson(List<Double> vector) {
        if (vector == null || vector.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(vector);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize vector to JSON", e);
            return "[]";
        }
    }

    public int getDefaultDimension() {
        EmbeddingProvider provider = providerFactory.getDefaultEmbeddingProvider();
        return provider != null ? provider.getDimension() : 1536;
    }
}