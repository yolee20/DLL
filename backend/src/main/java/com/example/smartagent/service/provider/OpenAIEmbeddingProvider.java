package com.example.smartagent.service.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class OpenAIEmbeddingProvider implements EmbeddingProvider {

    private final RestTemplate restTemplate;

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.embedding.model:text-embedding-3-small}")
    private String embeddingModel;

    public OpenAIEmbeddingProvider(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public String getProviderName() {
        return "openai";
    }

    @Override
    public List<Float> generateEmbedding(String text) {
        try {
            String url = "https://api.openai.com/v1/embeddings";

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("input", text);
            requestBody.put("model", embeddingModel);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().get("data");
            return (List<Float>) data.get(0).get("embedding");

        } catch (Exception e) {
            log.error("OpenAI embedding request failed", e);
            return List.of();
        }
    }

    @Override
    public int getDimension() {
        return switch (embeddingModel) {
            case "text-embedding-3-small" -> 1536;
            case "text-embedding-3-large" -> 3072;
            case "text-embedding-ada-002" -> 1536;
            default -> 1536;
        };
    }
}