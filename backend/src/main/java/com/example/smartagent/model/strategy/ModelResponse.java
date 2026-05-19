package com.example.smartagent.model.strategy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelResponse {
    private String output;
    private Double confidence;
    private Map<String, Object> metadata;
    
    public static ModelResponse success(String output) {
        return ModelResponse.builder()
                .output(output)
                .confidence(1.0)
                .build();
    }
    
    public static ModelResponse success(String output, Double confidence) {
        return ModelResponse.builder()
                .output(output)
                .confidence(confidence)
                .build();
    }

    public static ModelResponse failure(String output) {
        return ModelResponse.builder()
                .output(output)
                .confidence(0.0)
                .build();
    }
}
