package com.example.smartagent.mcp;

import com.example.smartagent.entity.ModelRegistry;
import com.example.smartagent.mcp.strategy.ModelStrategy;
import com.example.smartagent.mcp.strategy.ModelStrategyFactory;
import com.example.smartagent.repository.ModelRegistryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class ModelClient implements ModelControlProtocol {

    private final ModelRegistryRepository modelRegistryRepository;
    private final ModelStrategyFactory strategyFactory;
    private final Map<String, ModelStatus> loadedModels = new ConcurrentHashMap<>();

    public ModelClient(ModelRegistryRepository modelRegistryRepository,
                       ModelStrategyFactory strategyFactory) {
        this.modelRegistryRepository = modelRegistryRepository;
        this.strategyFactory = strategyFactory;
    }

    @Override
    public ModelResponse callModel(String modelName, ModelRequest request) {
        log.info("Calling model: {} with input: {}", modelName, request.getInput());

        ModelRegistry model = modelRegistryRepository.findByModelName(modelName)
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

    @Override
    public void loadModel(String modelName) {
        ModelRegistry model = modelRegistryRepository.findByModelName(modelName)
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

    @Override
    public void unloadModel(String modelName) {
        ModelRegistry model = modelRegistryRepository.findByModelName(modelName)
                .orElse(null);

        if (model != null) {
            model.setLoaded(false);
            modelRegistryRepository.save(model);
        }

        loadedModels.remove(modelName);
        log.info("Model unloaded: {}", modelName);
    }

    @Override
    public void reloadModel(String modelName) {
        unloadModel(modelName);
        loadModel(modelName);
        log.info("Model reloaded: {}", modelName);
    }

    @Override
    public boolean isModelLoaded(String modelName) {
        return loadedModels.containsKey(modelName) ||
                modelRegistryRepository.findByModelName(modelName)
                        .map(m -> Boolean.TRUE.equals(m.getLoaded()))
                        .orElse(false);
    }

    @Override
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

    @Override
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