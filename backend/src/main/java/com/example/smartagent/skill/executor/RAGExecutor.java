package com.example.smartagent.skill.executor;

import com.example.smartagent.entity.QAPair;
import com.example.smartagent.service.LlmService;
import com.example.smartagent.service.VectorStore;
import com.example.smartagent.skill.SkillResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class RAGExecutor implements SkillExecutor {

    private static final int TOP_K = 3;
    private static final double SIMILARITY_THRESHOLD = 0.3;

    private final VectorStore vectorStore;
    private final LlmService llmService;

    @Override
    public String getExecutorType() {
        return "rag";
    }

    @Override
    public SkillResult execute(String userMessage, Map<String, Object> context) {
        try {
            List<QAPair> similarPairs = vectorStore.searchSimilar(userMessage, TOP_K);

            if (similarPairs != null && !similarPairs.isEmpty()) {
                String contextStr = buildContext(similarPairs);
                String answer = llmService.generateAnswerWithContext(userMessage, contextStr);
                log.debug("RAG found {} similar pairs, generated answer", similarPairs.size());
                return SkillResult.success(answer, "RAGExecutor", 0.9);
            }

            log.debug("RAG no similar pairs found, falling back to direct LLM");
            String answer = llmService.generateAnswer(userMessage);
            return SkillResult.success(answer, "RAGExecutor", 0.8);

        } catch (Exception e) {
            log.error("RAG execution failed", e);
            return SkillResult.failure("问答服务暂时不可用: " + e.getMessage());
        }
    }

    @Override
    public boolean supports(String intent) {
        return "qa".equalsIgnoreCase(intent) ||
                "question".equalsIgnoreCase(intent) ||
                "help".equalsIgnoreCase(intent) ||
                "询问".equals(intent);
    }

    private String buildContext(List<QAPair> pairs) {
        StringBuilder context = new StringBuilder();
        for (QAPair pair : pairs) {
            context.append("问题: ").append(pair.getQuestion()).append("\n");
            context.append("答案: ").append(pair.getAnswer()).append("\n\n");
        }
        return context.toString();
    }
}