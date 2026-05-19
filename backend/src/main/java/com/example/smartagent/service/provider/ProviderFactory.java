package com.example.smartagent.service.provider;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ProviderFactory {

    private final List<EmbeddingProvider> embeddingProviders;
    private final List<LLMProvider> llmProviders;

    private final Map<String, EmbeddingProvider> embeddingProviderMap = new ConcurrentHashMap<>();
    private final Map<String, LLMProvider> llmProviderMap = new ConcurrentHashMap<>();

    public ProviderFactory(List<EmbeddingProvider> embeddingProviders,
                          List<LLMProvider> llmProviders) {
        this.embeddingProviders = embeddingProviders;
        this.llmProviders = llmProviders;
        this.embeddingProviderMap.putAll(embeddingProviders.stream()
                .collect(Collectors.toMap(EmbeddingProvider::getProviderName, Function.identity())));
        this.llmProviderMap.putAll(llmProviders.stream()
                .collect(Collectors.toMap(LLMProvider::getProviderName, Function.identity())));
    }

    public EmbeddingProvider getEmbeddingProvider(String name) {
        return embeddingProviderMap.getOrDefault(name, embeddingProviderMap.values().stream().findFirst().orElse(null));
    }

    public EmbeddingProvider getDefaultEmbeddingProvider() {
        return embeddingProviderMap.values().stream().findFirst().orElse(null);
    }

    public LLMProvider getLLMProvider(String name) {
        return llmProviderMap.getOrDefault(name, llmProviderMap.values().stream().findFirst().orElse(null));
    }

    public LLMProvider getDefaultLLMProvider() {
        return llmProviderMap.values().stream().findFirst().orElse(null);
    }

    public List<String> getSupportedEmbeddingProviders() {
        return List.copyOf(embeddingProviderMap.keySet());
    }

    public List<String> getSupportedLLMProviders() {
        return List.copyOf(llmProviderMap.keySet());
    }
}