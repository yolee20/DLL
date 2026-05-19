package com.example.smartagent.service;

import com.example.smartagent.service.provider.LLMProvider;
import com.example.smartagent.service.provider.ProviderFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class LlmService {

    private static final String SYSTEM_PROMPT_DEFAULT = """
            你是一个专业的智能客服助手，精通各种领域知识。
            请用简洁、友好的语言回答用户的问题。
            如果不确定答案，请如实说明。
            """;

    private static final String SYSTEM_PROMPT_RAG = """
            你是一个专业的智能客服助手。
            请根据提供的上下文信息回答用户的问题。
            如果上下文中没有相关信息，请直接回答，不要强行引用。
            请用简洁、友好的语言回答。
            """;

    private final ProviderFactory providerFactory;

    public String generateAnswer(String question) {
        LLMProvider provider = providerFactory.getDefaultLLMProvider();
        if (provider == null) {
            log.error("No LLM provider available");
            return "抱歉，服务暂时不可用";
        }
        return provider.generate(SYSTEM_PROMPT_DEFAULT + "\n\n用户问题：" + question);
    }

    public String generateAnswerWithContext(String question, String context) {
        LLMProvider provider = providerFactory.getDefaultLLMProvider();
        if (provider == null) {
            log.error("No LLM provider available");
            return "抱歉，服务暂时不可用";
        }
        return provider.generateWithContext(question, context);
    }

    public String generate(String prompt, String providerName) {
        LLMProvider provider = providerFactory.getLLMProvider(providerName);
        if (provider == null) {
            log.warn("Provider not found: {}, using default", providerName);
            return generateAnswer(prompt);
        }
        return provider.generate(prompt);
    }

    public String generateWithContext(String prompt, String context, String providerName) {
        LLMProvider provider = providerFactory.getLLMProvider(providerName);
        if (provider == null) {
            log.warn("Provider not found: {}, using default", providerName);
            return generateAnswerWithContext(prompt, context);
        }
        return provider.generateWithContext(prompt, context);
    }

    public void generateStream(String prompt, LLMProvider.StreamHandler handler) {
        LLMProvider provider = providerFactory.getDefaultLLMProvider();
        if (provider == null) {
            handler.onError(new RuntimeException("No LLM provider available"));
            return;
        }
        provider.generateStream(prompt, handler);
    }

    public String getProviderName() {
        LLMProvider provider = providerFactory.getDefaultLLMProvider();
        return provider != null ? provider.getProviderName() : "unknown";
    }
}