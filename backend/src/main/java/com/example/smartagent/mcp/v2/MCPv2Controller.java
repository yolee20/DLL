
package com.example.smartagent.mcp.v2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("/api/mcp/v2")
@RequiredArgsConstructor
@Slf4j
public class MCPv2Controller {
    
    private final ModelClient modelClient;
    
    @PostMapping("/predict")
    public ResponseEntity<ModelResponse> predict(@RequestBody ModelRequest request) {
        log.info("Received predict request for model: {}", request.getModelId());
        ModelResponse response = modelClient.predict(request.getModelId(), request.getPrompt());
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/chat")
    public ResponseEntity<ModelResponse> chat(@RequestBody ModelRequest request) {
        log.info("Received chat request for model: {}", request.getModelId());
        ModelResponse response = modelClient.predict(request.getModelId(), request.getMessages());
        return ResponseEntity.ok(response);
    }
    
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ModelResponse> streamPredict(@RequestBody ModelRequest request) {
        log.info("Received stream request for model: {}", request.getModelId());
        return modelClient.streamPredict(request.getModelId(), request.getPrompt());
    }
    
    @GetMapping("/models")
    public ResponseEntity<List<ModelStatus>> listModels() {
        List<ModelStatus> models = modelClient.listModels();
        return ResponseEntity.ok(models);
    }
    
    @GetMapping("/models/{modelId}")
    public ResponseEntity<ModelStatus> getModelStatus(@PathVariable String modelId) {
        ModelStatus status = modelClient.getModelStatus(modelId);
        if (status == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(status);
    }
    
    @PostMapping("/models")
    public ResponseEntity<Void> registerModel(@RequestBody ModelRegistration registration) {
        log.info("Registering model: {}", registration.getModelId());
        modelClient.registerModel(registration);
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/models/{modelId}")
    public ResponseEntity<Void> unregisterModel(@PathVariable String modelId) {
        log.info("Unregistering model: {}", modelId);
        modelClient.unregisterModel(modelId);
        return ResponseEntity.ok().build();
    }
}
