
package com.example.smartagent.mcp.v2;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Slf4j
public class StandardModelControlProtocol implements ModelControlProtocol {
    
    private final Map<String, ModelStatus> modelRegistry = new ConcurrentHashMap<>();
    
    private final Map<String, ModelRegistration> modelConfigurations = new ConcurrentHashMap<>();
    
    private final Map<String, AtomicLong> requestCounters = new ConcurrentHashMap<>();
    
    @Override
    public ModelResponse predict(ModelRequest request) {
        log.info("Predict request for model: {}", request.getModelId());
        
        if (!modelRegistry.containsKey(request.getModelId())) {
            return ModelResponse.builder()
                    .success(false)
                    .error("Model not found: " + request.getModelId())
                    .build();
        }
        
        incrementRequestCount(request.getModelId());
        
        String responseContent = generateResponse(request);
        
        return ModelResponse.builder()
                .success(true)
                .model(request.getModelId())
                .choices(List.of(ModelResponse.Choice.builder()
                        .index(0)
                        .message(ModelResponse.Message.builder()
                                .role("assistant")
                                .content(responseContent)
                                .build())
                        .finishReason("stop")
                        .build()))
                .usage(ModelResponse.Usage.builder()
                        .promptTokens(estimateTokens(request.getPrompt()))
                        .completionTokens(estimateTokens(responseContent))
                        .totalTokens(estimateTokens(request.getPrompt()) + estimateTokens(responseContent))
                        .build())
                .build();
    }
    
    @Override
    public Flux<ModelResponse> streamPredict(ModelRequest request) {
        log.info("Stream predict request for model: {}", request.getModelId());
        
        if (!modelRegistry.containsKey(request.getModelId())) {
            return Flux.error(new RuntimeException("Model not found: " + request.getModelId()));
        }
        
        incrementRequestCount(request.getModelId());
        
        String responseContent = generateResponse(request);
        List<String> chunks = splitIntoChunks(responseContent);
        
        return Flux.fromIterable(chunks)
                .map(chunk -> ModelResponse.builder()
                        .success(true)
                        .model(request.getModelId())
                        .choices(List.of(ModelResponse.Choice.builder()
                                .index(0)
                                .text(chunk)
                                .finishReason(chunks.indexOf(chunk) == chunks.size() - 1 ? "stop" : null)
                                .build()))
                        .build());
    }
    
    @Override
    public ModelStatus getModelStatus(String modelId) {
        return modelRegistry.get(modelId);
    }
    
    @Override
    public List<ModelStatus> listModels() {
        return new ArrayList<>(modelRegistry.values());
    }
    
    @Override
    public void registerModel(ModelRegistration registration) {
        log.info("Registering model: {}", registration.getModelId());
        
        ModelStatus status = ModelStatus.builder()
                .modelId(registration.getModelId())
                .modelName(registration.getModelName())
                .provider(registration.getProvider())
                .status("ACTIVE")
                .lastUsedTime(System.currentTimeMillis())
                .requestCount(0L)
                .avgResponseTime(0.0)
                .description(registration.getMetadata() != null ? 
                        registration.getMetadata().get("description") : null)
                .config(registration.getDefaultConfig())
                .build();
        
        modelRegistry.put(registration.getModelId(), status);
        modelConfigurations.put(registration.getModelId(), registration);
        requestCounters.put(registration.getModelId(), new AtomicLong(0));
        
        log.info("Model registered successfully: {}", registration.getModelId());
    }
    
    @Override
    public void unregisterModel(String modelId) {
        log.info("Unregistering model: {}", modelId);
        modelRegistry.remove(modelId);
        modelConfigurations.remove(modelId);
        requestCounters.remove(modelId);
    }
    
    private String generateResponse(ModelRequest request) {
        String prompt = request.getPrompt();
        if (prompt == null && request.getMessages() != null && !request.getMessages().isEmpty()) {
            prompt = request.getMessages().stream()
                    .map(m -> m.getRole() + ": " + m.getContent())
                    .reduce((a, b) -> a + "\n" + b)
                    .orElse("");
        }
        
        return "Response from model " + request.getModelId() + 
               " for prompt: " + (prompt != null && prompt.length() > 50 ? prompt.substring(0, 50) + "..." : prompt);
    }
    
    private List<String> splitIntoChunks(String content) {
        List<String> chunks = new ArrayList<>();
        int chunkSize = 10;
        for (int i = 0; i < content.length(); i += chunkSize) {
            chunks.add(content.substring(i, Math.min(i + chunkSize, content.length())));
        }
        return chunks;
    }
    
    private Integer estimateTokens(String text) {
        return text != null ? (int) (text.length() / 4.0) : 0;
    }
    
    private void incrementRequestCount(String modelId) {
        requestCounters.computeIfAbsent(modelId, k -> new AtomicLong(0)).incrementAndGet();
        modelRegistry.computeIfPresent(modelId, (id, status) -> {
            status.setRequestCount(requestCounters.get(id).get());
            status.setLastUsedTime(System.currentTimeMillis());
            return status;
        });
    }
}
