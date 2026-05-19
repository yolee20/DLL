package com.example.smartagent.service.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Component
@Slf4j
public class OpenAILLMProvider implements LLMProvider {

    private final RestTemplate restTemplate;

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.chat.model:gpt-4o-mini}")
    private String chatModel;

    public OpenAILLMProvider(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public String getProviderName() {
        return "openai";
    }

    @Override
    public String generate(String prompt) {
        try {
            String url = "https://api.openai.com/v1/chat/completions";

            List<Map<String, Object>> messages = List.of(
                    Map.of("role", "user", "content", prompt)
            );

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", chatModel);
            requestBody.put("messages", messages);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            Map<String, Object> body = response.getBody();
            if (body == null) {
                return "抱歉，生成回答失败";
            }

            List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");
            if (choices == null || choices.isEmpty()) {
                return "抱歉，生成回答失败";
            }

            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            return (String) message.get("content");

        } catch (Exception e) {
            log.error("OpenAI chat request failed", e);
            return "抱歉，服务暂时不可用";
        }
    }

    @Override
    public String generateWithContext(String prompt, String context) {
        String fullPrompt = buildRAGPrompt(context, prompt);
        return generate(fullPrompt);
    }

    @Override
    public StreamResponse generateStream(String prompt, StreamHandler handler) {
        try {
            String url = "https://api.openai.com/v1/chat/completions";

            List<Map<String, Object>> messages = List.of(
                    Map.of("role", "user", "content", prompt)
            );

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", chatModel);
            requestBody.put("messages", messages);
            requestBody.put("stream", true);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getBody() != null) {
                handler.onComplete();
            }

            return StreamResponse.started();
        } catch (Exception e) {
            log.error("OpenAI stream request failed", e);
            handler.onError(e);
            return new StreamResponse(false);
        }
    }
}