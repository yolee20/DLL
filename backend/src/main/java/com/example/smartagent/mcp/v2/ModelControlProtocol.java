
package com.example.smartagent.mcp.v2;

import reactor.core.publisher.Flux;

import java.util.List;

public interface ModelControlProtocol {
    
    ModelResponse predict(ModelRequest request);
    
    Flux<ModelResponse> streamPredict(ModelRequest request);
    
    ModelStatus getModelStatus(String modelId);
    
    List<ModelStatus> listModels();
    
    void registerModel(ModelRegistration registration);
    
    void unregisterModel(String modelId);
}
