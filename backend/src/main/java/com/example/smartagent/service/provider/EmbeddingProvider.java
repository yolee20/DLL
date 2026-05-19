package com.example.smartagent.service.provider;

import java.util.List;

public interface EmbeddingProvider {

    String getProviderName();

    List<Float> generateEmbedding(String text);

    int getDimension();

    default double calculateSimilarity(List<Double> vec1, List<Double> vec2) {
        if (vec1.isEmpty() || vec2.isEmpty()) return 0;

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
}