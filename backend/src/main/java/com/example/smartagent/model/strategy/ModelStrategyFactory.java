package com.example.smartagent.model.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ModelStrategyFactory {

    private final List<ModelStrategy> strategies;
    private final Map<String, ModelStrategy> strategyMap = new ConcurrentHashMap<>();

    public ModelStrategyFactory(List<ModelStrategy> strategies) {
        this.strategies = strategies;
        this.strategyMap.putAll(strategies.stream()
                .collect(Collectors.toMap(ModelStrategy::getModelType, Function.identity())));
    }

    public ModelStrategy getStrategy(String modelType) {
        ModelStrategy strategy = strategyMap.get(modelType.toLowerCase());
        if (strategy == null) {
            log.warn("Strategy not found for model type: {}, using default", modelType);
            return strategyMap.values().stream()
                    .findFirst()
                    .orElse(null);
        }
        return strategy;
    }

    public ModelStrategy getStrategyOrDefault(String modelType, ModelStrategy defaultStrategy) {
        ModelStrategy strategy = getStrategy(modelType);
        return strategy != null ? strategy : defaultStrategy;
    }

    public List<String> getSupportedModelTypes() {
        return List.copyOf(strategyMap.keySet());
    }
}
