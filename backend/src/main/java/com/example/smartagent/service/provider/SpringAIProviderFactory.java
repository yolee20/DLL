package com.example.smartagent.service.provider;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.StreamingChatModel;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class SpringAIProviderFactory {

    private final List<LLMProvider> llmProviders;
    private final List<EmbeddingProvider> embeddingProviders;

    private final Map<String, LLMProvider> llmProviderMap = new ConcurrentHashMap<>();
    private final Map<String, EmbeddingProvider> embeddingProviderMap = new ConcurrentHashMap<>();

    private final ChatModel chatModel;
    private final StreamingChatModel streamingChatModel;
    private ChatClient chatClient;

    public SpringAIProviderFactory(List<LLMProvider> llmProviders,
                                 List<EmbeddingProvider> embeddingProviders,
                                 ChatModel chatModel,
                                 StreamingChatModel streamingChatModel) {
        this.llmProviders = llmProviders;
        this.embeddingProviders = embeddingProviders;
        this.chatModel = chatModel;
        this.streamingChatModel = streamingChatModel;

        this.llmProviderMap.putAll(llmProviders.stream()
                .collect(Collectors.toMap(LLMProvider::getProviderName, Function.identity())));
        this.embeddingProviderMap.putAll(embeddingProviders.stream()
                .collect(Collectors.toMap(EmbeddingProvider::getProviderName, Function.identity())));

        initChatClient();
    }

    private void initChatClient() {
        if (chatModel != null) {
            this.chatClient = ChatClient.builder(chatModel).build();
        }
    }

    public ChatClient getChatClient() {
        return chatClient;
    }

    public ChatModel getChatModel() {
        return chatModel;
    }

    public StreamingChatModel getStreamingChatModel() {
        return streamingChatModel;
    }

    public LLMProvider getLLMProvider(String name) {
        return llmProviderMap.getOrDefault(name, llmProviderMap.values().stream().findFirst().orElse(null));
    }

    public LLMProvider getDefaultLLMProvider() {
        return llmProviderMap.values().stream()
                .filter(p -> p.getProviderName().contains("springai"))
                .findFirst()
                .orElse(llmProviderMap.values().stream().findFirst().orElse(null));
    }

    public EmbeddingProvider getEmbeddingProvider(String name) {
        return embeddingProviderMap.getOrDefault(name, embeddingProviderMap.values().stream().findFirst().orElse(null));
    }

    public EmbeddingProvider getDefaultEmbeddingProvider() {
        return embeddingProviderMap.values().stream()
                .filter(p -> p.getProviderName().contains("springai"))
                .findFirst()
                .orElse(embeddingProviderMap.values().stream().findFirst().orElse(null));
    }

    public List<String> getSupportedLLMProviders() {
        return List.copyOf(llmProviderMap.keySet());
    }

    public List<String> getSupportedEmbeddingProviders() {
        return List.copyOf(embeddingProviderMap.keySet());
    }
}
