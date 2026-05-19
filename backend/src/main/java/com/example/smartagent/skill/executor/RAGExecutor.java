package com.example.smartagent.skill.executor;

import com.example.smartagent.service.rag.RAGEngine;
import com.example.smartagent.service.rag.RAGResult;
import com.example.smartagent.skill.SkillResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class RAGExecutor implements SkillExecutor {

    private final RAGEngine ragEngine;

    @Override
    public String getExecutorType() {
        return "rag";
    }

    @Override
    public SkillResult execute(String userMessage, Map<String, Object> context) {
        try {
            RAGResult result = ragEngine.retrieveOrGenerate(userMessage);
            log.debug("RAG execution completed, confidence: {}", result.getConfidence());
            return SkillResult.success(result.getAnswer(), "RAGExecutor", result.getConfidence());
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
}