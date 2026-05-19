
package com.example.smartagent.mcp.v2;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class MCPv2Config {
    
    private final ModelClient modelClient;
    
    @Value("${app.mcp.default-models.enabled:true}")
    private boolean enableDefaultModels;
    
    @PostConstruct
    public void init() {
        if (enableDefaultModels) {
            registerDefaultModels();
        }
    }
    
    private void registerDefaultModels() {
        log.info("Registering default models");
        
        ModelRegistration openaiRegistration = ModelRegistration.builder()
                .modelId("gpt-4")
                .modelName("GPT-4")
                .provider("openai")
                .baseUrl("https://api.openai.com/v1")
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .defaultConfig(ModelStatus.ModelConfig.builder()
                        .maxTokens(8192)
                        .temperature(0.7)
                        .topP(1.0)
                        .contextWindow(8192)
                        .build())
                .build();
        modelClient.registerModel(openaiRegistration);
        
        ModelRegistration claudeRegistration = ModelRegistration.builder()
                .modelId("claude-3-sonnet")
                .modelName("Claude 3 Sonnet")
                .provider("anthropic")
                .baseUrl("https://api.anthropic.com/v1")
                .apiKey(System.getenv("ANTHROPIC_API_KEY"))
                .defaultConfig(ModelStatus.ModelConfig.builder()
                        .maxTokens(200000)
                        .temperature(0.7)
                        .topP(1.0)
                        .contextWindow(200000)
                        .build())
                .build();
        modelClient.registerModel(claudeRegistration);
        
        log.info("Default models registered successfully");
    }
}
