package com.example.smartagent.agent.core;

import com.example.smartagent.dto.request.ChatRequest;
import com.example.smartagent.dto.response.ChatResponse;
import com.example.smartagent.service.rag.SpringAIEnhancedRAGEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class SpringAISmartAgent {

    private final ChatClient chatClient;
    private final SpringAIEnhancedRAGEngine ragEngine;
    private final AgentStateMachine stateMachine;

    private static final String SYSTEM_PROMPT = """
            你是一个专业、友好、智能的AI助手。
            你的职责是帮助用户解决问题，提供准确、有用的回答。
            保持回答简洁明了，专业准确。
            """;

    public ChatResponse chat(ChatRequest request) {
        String sessionId = request.getSessionId() != null && !request.getSessionId().isEmpty()
                ? request.getSessionId()
                : UUID.randomUUID().toString();

        log.info("Spring AI SmartAgent: Session {} chat", sessionId);

        stateMachine.init(sessionId);

        try {
            String ragAnswer = ragEngine.retrieveAndGenerate(request.getMessage());

            String response = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user("用户问题: " + request.getMessage() + "\n\n知识库参考信息: " + ragAnswer)
                    .call()
                    .content();

            stateMachine.end(sessionId);

            return ChatResponse.builder()
                    .response(response)
                    .sessionId(sessionId)
                    .intent("chat")
                    .intentConfidence(0.95)
                    .skillUsed("SpringAISmartAgent")
                    .build();

        } catch (Exception e) {
            log.error("Spring AI SmartAgent: Chat error", e);
            stateMachine.error(sessionId);
            return ChatResponse.builder()
                    .response("抱歉，服务暂时不可用，请稍后重试")
                    .sessionId(sessionId)
                    .intent("error")
                    .intentConfidence(0.0)
                    .skillUsed("error")
                    .build();
        }
    }

    public Flux<String> chatStream(ChatRequest request) {
        String sessionId = request.getSessionId() != null && !request.getSessionId().isEmpty()
                ? request.getSessionId()
                : UUID.randomUUID().toString();

        log.info("Spring AI SmartAgent: Session {} stream chat", sessionId);

        stateMachine.init(sessionId);

        try {
            String ragAnswer = ragEngine.retrieveAndGenerate(request.getMessage());

            return chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user("用户问题: " + request.getMessage() + "\n\n知识库参考信息: " + ragAnswer)
                    .stream()
                    .content()
                    .doOnComplete(() -> {
                        stateMachine.end(sessionId);
                        log.info("Spring AI SmartAgent: Stream complete");
                    })
                    .doOnError(e -> {
                        stateMachine.error(sessionId);
                        log.error("Spring AI SmartAgent: Stream error", e);
                    });
        } catch (Exception e) {
            log.error("Spring AI SmartAgent: Stream error", e);
            stateMachine.error(sessionId);
            return Flux.just("抱歉，服务暂时不可用");
        }
    }

    public ChatResponse ragChat(ChatRequest request) {
        String sessionId = request.getSessionId() != null && !request.getSessionId().isEmpty()
                ? request.getSessionId()
                : UUID.randomUUID().toString();

        log.info("Spring AI SmartAgent: Session {} RAG chat", sessionId);

        String answer = ragEngine.retrieveAndGenerate(request.getMessage());

        return ChatResponse.builder()
                .response(answer)
                .sessionId(sessionId)
                .intent("rag")
                .intentConfidence(0.98)
                .skillUsed("SpringAIRAG")
                .build();
    }

    public void addKnowledgeDocuments(List<Document> documents) {
        log.info("Spring AI SmartAgent: Adding {} documents to KB", documents.size());
        ragEngine.addDocuments(documents);
    }

    public void deleteKnowledgeDocuments(List<String> docIds) {
        log.info("Spring AI SmartAgent: Deleting {} documents from KB", docIds.size());
        ragEngine.deleteDocuments(docIds);
    }
}
