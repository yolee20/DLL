package com.example.smartagent.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.StreamingChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringAIConfig {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    @Value("${spring.ai.openai.chat.model:gpt-4o-mini}")
    private String chatModel;

    @Value("${spring.ai.openai.embedding.model:text-embedding-3-small}")
    private String embeddingModel;

    @Bean
    public ChatModel chatModel(ChatModel chatModel) {
        return chatModel;
    }

    @Bean
    public StreamingChatModel streamingChatModel(StreamingChatModel streamingChatModel) {
        return streamingChatModel;
    }

    @Bean
    public EmbeddingModel embeddingModel(EmbeddingModel embeddingModel) {
        return embeddingModel;
    }
}
