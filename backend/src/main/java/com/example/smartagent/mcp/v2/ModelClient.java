
package com.example.smartagent.mcp.v2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class ModelClient {
    
    private final ModelControlProtocol modelControlProtocol;
    
    public ModelResponse predict(String modelId, String prompt) {
        log.debug("Predicting with model: {}, prompt: {}", modelId, prompt);
        
        ModelRequest request = ModelRequest.builder()
                .modelId(modelId)
                .prompt(prompt)
                .temperature(0.7)
                .maxTokens(1024)
                .build();
        
        return modelControlProtocol.predict(request);
    }
    
    public ModelResponse predict(String modelId, List<ModelRequest.Message> messages) {
        log.debug("Predicting with model: {}, messages: {}", modelId, messages);
        
        ModelRequest request = ModelRequest.builder()
                .modelId(modelId)
                .messages(messages)
                .temperature(0.7)
                .maxTokens(1024)
                .build();
        
        return modelControlProtocol.predict(request);
    }
    
    public Flux<ModelResponse> streamPredict(String modelId, String prompt) {
        log.debug("Streaming predict with model: {}, prompt: {}", modelId, prompt);
        
        ModelRequest request = ModelRequest.builder()
                .modelId(modelId)
                .prompt(prompt)
                .temperature(0.7)
                .maxTokens(1024)
                .build();
        
        return modelControlProtocol.streamPredict(request);
    }
    
    public ModelStatus getModelStatus(String modelId) {
        return modelControlProtocol.getModelStatus(modelId);
    }
    
    public List<ModelStatus> listModels() {
        return modelControlProtocol.listModels();
    }
    
    public void registerModel(ModelRegistration registration) {
        modelControlProtocol.registerModel(registration);
    }
    
    public void unregisterModel(String modelId) {
        modelControlProtocol.unregisterModel(modelId);
    }
}
