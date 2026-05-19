package com.example.smartagent.service.provider;

import java.util.Map;

public interface LLMProvider {

    String getProviderName();

    String generate(String prompt);

    String generateWithContext(String prompt, String context);

    StreamResponse generateStream(String prompt, StreamHandler handler);

    interface StreamHandler {
        void onChunk(String chunk);
        void onComplete();
        void onError(Throwable error);
    }

    record StreamResponse(boolean status) {
        public static StreamResponse started() {
            return new StreamResponse(true);
        }
    }

    default String buildPrompt(String systemPrompt, String userPrompt) {
        return String.format("""
                <system>
                %s
                </system>
                <user>
                %s
                </user>
                """, systemPrompt, userPrompt);
    }

    default String buildRAGPrompt(String context, String question) {
        return String.format("""
                <context>
                %s
                </context>
                <question>
                %s
                </question>
                根据上下文信息回答问题。如果上下文中没有相关信息，请基于你的知识回答。
                """, context, question);
    }
}