package com.example.smartagent.skill.executor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
public class SkillExecutorFactory {

    private final Map<String, SkillExecutor> executorMap = new ConcurrentHashMap<>();
    private SkillExecutor defaultExecutor;

    public SkillExecutorFactory(List<SkillExecutor> executors) {
        for (SkillExecutor executor : executors) {
            executorMap.put(executor.getExecutorType(), executor);
            if (defaultExecutor == null) {
                defaultExecutor = executor;
            }
        }
        log.info("Initialized {} skill executors: {}",
                executorMap.size(), executorMap.keySet());
    }

    public SkillExecutor getExecutor(String type) {
        return executorMap.getOrDefault(type, defaultExecutor);
    }

    public SkillExecutor getExecutorForIntent(String intent) {
        return executorMap.values().stream()
                .filter(e -> e.supports(intent))
                .findFirst()
                .orElse(defaultExecutor);
    }

    public SkillExecutor getDefaultExecutor() {
        return defaultExecutor;
    }

    public List<String> getSupportedExecutorTypes() {
        return List.copyOf(executorMap.keySet());
    }
}