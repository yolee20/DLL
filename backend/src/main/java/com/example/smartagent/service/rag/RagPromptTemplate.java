package com.example.smartagent.service.rag;

import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class RagPromptTemplate {

    private static final String RAG_PROMPT_TEMPLATE = """
        请根据以下参考信息回答用户的问题。
        
        参考信息：
        {context}
        
        用户问题：{query}
        
        要求：
        1. 如果参考信息中有相关内容，请基于参考信息回答
        2. 如果参考信息中没有相关内容，请直接回答用户问题
        3. 回答要简洁明了，避免冗余
        """;

    private static final String EMPTY_CONTEXT_PROMPT_TEMPLATE = """
        用户问题：{query}
        
        请直接回答用户的问题。
        """;

    private final PromptTemplate ragPromptTemplate;
    private final PromptTemplate emptyContextPromptTemplate;

    public RagPromptTemplate() {
        this.ragPromptTemplate = new PromptTemplate(RAG_PROMPT_TEMPLATE);
        this.emptyContextPromptTemplate = new PromptTemplate(EMPTY_CONTEXT_PROMPT_TEMPLATE);
    }

    public String buildRagPrompt(String query, List<ScoredMatch> contextMatches) {
        if (contextMatches == null || contextMatches.isEmpty()) {
            return emptyContextPromptTemplate.create(Map.of("query", query)).getContents();
        }

        String context = formatContext(contextMatches);
        Map<String, Object> variables = new HashMap<>();
        variables.put("query", query);
        variables.put("context", context);

        return ragPromptTemplate.create(variables).getContents();
    }

    private String formatContext(List<ScoredMatch> matches) {
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < matches.size(); i++) {
            ScoredMatch match = matches.get(i);
            context.append(String.format("""
                
                参考 %d:
                问题: %s
                回答: %s
                来源: %s
                置信度: %.2f
                """,
                i + 1,
                match.getQuestion(),
                match.getAnswer(),
                match.getSource(),
                match.getScore()
            ));
        }
        return context.toString();
    }
}
