package com.example.smartagent.service.llm;

import com.example.smartagent.entity.ModelRegistryEntity;
import com.example.smartagent.model.strategy.ModelStrategy;
import com.example.smartagent.model.strategy.ModelStrategyFactory;
import com.example.smartagent.model.strategy.ModelRequest;
import com.example.smartagent.model.strategy.ModelResponse;
import com.example.smartagent.model.strategy.ModelStatus;
import com.example.smartagent.repository.ModelRegistryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class ModelService {

    private final ModelRegistryRepository modelRegistryRepository;
    private final ModelStrategyFactory strategyFactory;
    private final Map<String, ModelStatus> loadedModels = new ConcurrentHashMap<>();
    private final Map<String, List<String>> modelGroups = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> roundRobinCounters = new ConcurrentHashMap<>();

    public void initModelGroups() {
        modelGroups.put("llm", List.of("gpt-4", "gpt-3.5-turbo", "qwen-max"));
        modelGroups.put("embedding", List.of("text-embedding-ada-002", "text-embedding-v3"));
        roundRobinCounters.put("llm", new AtomicInteger(0));
        roundRobinCounters.put("embedding", new AtomicInteger(0));
    }

    public ModelResponse callModel(String modelName, ModelRequest request) {
        return callModelWithFallback(modelName, request, 3);
    }

    public ModelResponse callModelWithFallback(String modelName, ModelRequest request, int maxRetries) {
        log.info("Calling model: {} with input: {}", modelName, request.getInput());

        int retryCount = 0;
        Exception lastException = null;

        while (retryCount < maxRetries) {
            try {
                ModelResponse response = callModelInternal(modelName, request);
                if (response != null && response.getOutput() != null) {
                    return response;
                }
            } catch (Exception e) {
                lastException = e;
                log.warn("Model call failed, retrying: {}/{}", retryCount + 1, maxRetries, e);
            }

            retryCount++;
            if (retryCount < maxRetries) {
                modelName = getFallbackModel(modelName);
            }
        }

        log.error("All retries exhausted for model call");
        return ModelResponse.failure("All retries exhausted: " + (lastException != null ? lastException.getMessage() : "unknown error"));
    }

    private String getFallbackModel(String currentModel) {
        List<String> group = modelGroups.get("llm");
        if (group == null || group.isEmpty()) {
            return "gpt-3.5-turbo";
        }

        int index = roundRobinCounters.get("llm").getAndIncrement() % group.size();
        String fallback = group.get(index);
        
        if (fallback.equals(currentModel)) {
            index = (index + 1) % group.size();
            fallback = group.get(index);
        }
        
        log.info("Using fallback model: {}", fallback);
        return fallback;
    }

    private ModelResponse callModelInternal(String modelName, ModelRequest request) {
        ModelRegistryEntity model = modelRegistryRepository.findByModelName(modelName)
                .orElse(null);

        if (model == null) {
            log.warn("Model not found in registry: {}, using default strategy", modelName);
            return executeWithDefaultStrategy(modelName, request.getInput());
        }

        if (!Boolean.TRUE.equals(model.getLoaded())) {
            loadModel(modelName);
        }

        return executeModel(model.getModelType(), request.getInput());
    }

    private ModelResponse executeWithDefaultStrategy(String modelName, String input) {
        ModelStrategy strategy = strategyFactory.getStrategy(modelName.toLowerCase());
        if (strategy != null) {
            return executeStrategy(strategy, input);
        }

        log.warn("No strategy found for model: {}, returning fallback response", modelName);
        return ModelResponse.builder()
                .output("fallback_response")
                .confidence(0.5)
                .build();
    }

    private ModelResponse executeModel(String modelType, String input) {
        ModelStrategy strategy = strategyFactory.getStrategy(modelType.toLowerCase());
        if (strategy == null) {
            log.warn("Strategy not found for model type: {}", modelType);
            return ModelResponse.failure("Strategy not found for model type: " + modelType);
        }
        return executeStrategy(strategy, input);
    }

    private ModelResponse executeStrategy(ModelStrategy strategy, String input) {
        try {
            ModelStrategy.ModelOutput output = strategy.execute(input);
            return ModelResponse.success(output.output(), output.confidence());
        } catch (Exception e) {
            log.error("Strategy execution failed", e);
            return ModelResponse.failure("Strategy execution failed: " + e.getMessage());
        }
    }

    public void loadModel(String modelName) {
        ModelRegistryEntity model = modelRegistryRepository.findByModelName(modelName)
                .orElse(null);

        if (model == null) {
            log.warn("Model not found in registry: {}", modelName);
            return;
        }

        ModelStrategy strategy = strategyFactory.getStrategy(model.getModelType());
        if (strategy == null) {
            log.warn("No strategy for model type: {}", model.getModelType());
            return;
        }

        model.setLoaded(true);
        modelRegistryRepository.save(model);

        loadedModels.put(modelName, ModelStatus.loaded(modelName, "1.0.0", model.getModelType()));
        log.info("Model loaded: {}", modelName);
    }

    public void unloadModel(String modelName) {
        ModelRegistryEntity model = modelRegistryRepository.findByModelName(modelName)
                .orElse(null);

        if (model != null) {
            model.setLoaded(false);
            modelRegistryRepository.save(model);
        }

        loadedModels.remove(modelName);
        log.info("Model unloaded: {}", modelName);
    }

    public void reloadModel(String modelName) {
        unloadModel(modelName);
        loadModel(modelName);
        log.info("Model reloaded: {}", modelName);
    }

    public boolean isModelLoaded(String modelName) {
        return loadedModels.containsKey(modelName) ||
                modelRegistryRepository.findByModelName(modelName)
                        .map(m -> Boolean.TRUE.equals(m.getLoaded()))
                        .orElse(false);
    }

    public ModelStatus getModelStatus(String modelName) {
        ModelStatus cached = loadedModels.get(modelName);
        if (cached != null) {
            return cached;
        }

        return modelRegistryRepository.findByModelName(modelName)
                .map(m -> ModelStatus.builder()
                        .name(m.getModelName())
                        .version("1.0.0")
                        .type(m.getModelType())
                        .status(m.getLoaded() ? "loaded" : "unloaded")
                        .build())
                .orElse(null);
    }

    public List<ModelStatus> getAllModelStatus() {
        return modelRegistryRepository.findAll().stream()
                .map(m -> ModelStatus.builder()
                        .name(m.getModelName())
                        .version("1.0.0")
                        .type(m.getModelType())
                        .status(m.getLoaded() ? "loaded" : "unloaded")
                        .build())
                .toList();
    }
}
