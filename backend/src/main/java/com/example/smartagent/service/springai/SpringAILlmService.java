package com.example.smartagent.service.springai;

import com.example.smartagent.service.provider.SpringAILLMProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
@Slf4j
public class SpringAILlmService {

    private final SpringAILLMProvider llmProvider;

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

    public interface StreamHandler {
        void onChunk(String chunk);
        void onComplete();
        void onError(Exception e);
    }

    public String generateAnswer(String question) {
        String prompt = SYSTEM_PROMPT_DEFAULT + "\n\n用户问题：" + question;
        return llmProvider.generate(prompt);
    }

    public String generateAnswerWithContext(String question, String context) {
        String fullPrompt = buildRAGPrompt(context, question);
        return llmProvider.generate(fullPrompt);
    }

    public void generateStream(String prompt, StreamHandler handler) {
        llmProvider.generateStream(prompt, new com.example.smartagent.service.provider.LLMProvider.StreamHandler() {
            @Override
            public void onChunk(String chunk) {
                handler.onChunk(chunk);
            }

            @Override
            public void onComplete() {
                handler.onComplete();
            }

            @Override
            public void onError(Throwable error) {
                handler.onError(error instanceof Exception ? (Exception) error : new RuntimeException(error));
            }
        });
    }

    public String getProviderName() {
        return llmProvider.getProviderName();
    }

    private String buildRAGPrompt(String context, String question) {
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
