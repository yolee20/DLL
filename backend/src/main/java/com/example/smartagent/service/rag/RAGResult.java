package com.example.smartagent.service.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RAGResult {

    private String answer;

    private boolean generated;

    private double confidence;

    private String matchType;

    private List<ScoredMatch> matchedItems;

    public static RAGResult fromMatch(ScoredMatch match) {
        return RAGResult.builder()
                .answer(match.getAnswer())
                .generated(false)
                .confidence(match.getScore())
                .matchType("RETRIEVED")
                .matchedItems(List.of(match))
                .build();
    }

    public static RAGResult generated(String answer, double confidence) {
        return RAGResult.builder()
                .answer(answer)
                .generated(true)
                .confidence(confidence)
                .matchType("LLM_GENERATED")
                .build();
    }

    public static RAGResult noMatch() {
        return RAGResult.builder()
                .answer("抱歉，未找到相关信息，请换个问题尝试")
                .generated(false)
                .confidence(0.0)
                .matchType("NO_MATCH")
                .build();
    }
}
