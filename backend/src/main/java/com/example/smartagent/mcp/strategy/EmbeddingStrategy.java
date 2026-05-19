package com.example.smartagent.mcp.strategy;

import com.example.smartagent.service.EmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmbeddingStrategy implements ModelStrategy {

    private final EmbeddingService embeddingService;

    @Override
    public String getModelType() {
        return "embedding";
    }

    @Override
    public ModelOutput execute(String input) {
        log.debug("Generating embedding for input: {}", input);

        try {
            List<Float> embedding = embeddingService.generateEmbedding(input);
            double[] normalized = normalize(embedding);
            String vectorString = arrayToString(normalized);

            return ModelOutput.success(vectorString, 1.0);
        } catch (Exception e) {
            log.error("Failed to generate embedding", e);
            return new ModelOutput("", 0.0);
        }
    }

    private double[] normalize(List<Float> vector) {
        double norm = 0.0;
        for (double v : vector) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);

        if (norm == 0) {
            return new double[vector.size()];
        }

        double[] result = new double[vector.size()];
        for (int i = 0; i < vector.size(); i++) {
            result[i] = vector.get(i) / norm;
        }
        return result;
    }

    private String arrayToString(double[] array) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < array.length; i++) {
            sb.append(array[i]);
            if (i < array.length - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}