package com.example.smartagent.service.rag;

import com.example.smartagent.service.vector.VectorStore;
import com.example.smartagent.entity.QAPairEntity;
import com.example.smartagent.service.embedding.EmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 基于Spring AI的RAG引擎 - 使用项目现有VectorStore
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SpringAIEnhancedRAGEngine {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final EmbeddingService embeddingService;

    private static final String RAG_PROMPT_TEMPLATE = """
            你是一个专业的AI助手。请根据以下上下文信息回答用户的问题。
            
            上下文信息:
            {context}
            
            用户问题: {query}
            
            要求:
            1. 只根据提供的上下文信息回答，不要编造信息
            2. 如果上下文中没有相关信息，直接告知用户"抱歉，知识库中没有找到相关信息"
            3. 回答要简洁明了，准确专业
            """;

    public String retrieveAndGenerate(String query) {
        log.info("Spring AI RAG: Query - {}", query);

        List<String> relevantDocs = retrieveDocuments(query);
        String context = buildContext(relevantDocs);

        String answer = generateAnswer(query, context);
        log.info("Spring AI RAG: Answer generated");

        return answer;
    }

    public void addDocuments(List<org.springframework.ai.document.Document> documents) {
        log.info("Spring AI RAG: Adding {} documents", documents.size());
        // 这里需要根据实际情况实现，比如将Document转换为QAPair并存储
        for (int i = 0; i < documents.size(); i++) {
            org.springframework.ai.document.Document doc = documents.get(i);
            // 使用简单的ID生成方式
            vectorStore.storeVector((long) (i + 1), doc.getText());
        }
    }

    public void deleteDocuments(List<String> docIds) {
        log.info("Spring AI RAG: Deleting {} documents", docIds.size());
        for (String id : docIds) {
            try {
                Long qaId = Long.parseLong(id);
                vectorStore.deleteVector(qaId);
            } catch (NumberFormatException e) {
                log.warn("Invalid doc ID: {}", id);
            }
        }
    }

    private List<String> retrieveDocuments(String query) {
        List<QAPairEntity> qaPairs = vectorStore.searchSimilar(query, 5);
        log.info("Spring AI RAG: Found {} relevant documents", qaPairs.size());
        
        List<String> results = qaPairs.stream()
                .map(qa -> "Q: " + qa.getQuestion() + "\nA: " + qa.getAnswer())
                .collect(Collectors.toList());
        return results;
    }

    private String buildContext(List<String> documents) {
        if (documents.isEmpty()) {
            return "无相关上下文信息";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < documents.size(); i++) {
            sb.append("文档 ").append(i + 1).append(":\n");
            sb.append(documents.get(i)).append("\n\n");
        }
        return sb.toString();
    }

    private String generateAnswer(String query, String context) {
        PromptTemplate template = new PromptTemplate(RAG_PROMPT_TEMPLATE);
        Prompt prompt = template.create(Map.of("query", query, "context", context));
        return chatClient.prompt(prompt).call().content();
    }
}
