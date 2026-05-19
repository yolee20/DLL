package com.example.smartagent.service.rag;

import com.example.smartagent.entity.QAPairEntity;
import com.example.smartagent.service.springai.SpringAIEmbeddingService;
import com.example.smartagent.service.springai.SpringAIVectorStore;
import com.example.smartagent.service.springai.SpringAILlmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SpringAIRAGEngine {

    private final SpringAIEmbeddingService embeddingService;
    private final SpringAIVectorStore vectorStore;
    private final SpringAILlmService llmService;

    @Value("${rag.threshold:0.75}")
    private double similarityThreshold;

    @Value("${rag.top-k:5}")
    private int topK;

    public RAGResult retrieveOrGenerate(String query) {
        return retrieveOrGenerate(query, topK, similarityThreshold);
    }

    public RAGResult retrieveOrGenerate(String query, int topK, double threshold) {
        log.info("[SpringAI RAG] Query: {}, topK: {}, threshold: {}", query, topK, threshold);

        try {
            List<QAPairEntity> matches = vectorStore.searchSimilar(query, topK);

            if (!matches.isEmpty()) {
                QAPairEntity bestMatch = matches.get(0);
                List<Float> queryVector = embeddingService.generateEmbedding(query);
                List<Float> matchVector = embeddingService.parseVectorJson(bestMatch.getVectorJson());

                double similarity = embeddingService.calculateSimilarity(queryVector, matchVector);

                log.info("[SpringAI RAG] Found {} matches, best score: {}", matches.size(), similarity);

                if (similarity >= threshold) {
                    return RAGResult.fromMatch(ScoredMatch.builder()
                            .id(bestMatch.getId().toString())
                            .question(bestMatch.getQuestion())
                            .answer(bestMatch.getAnswer())
                            .category(bestMatch.getCategory() != null ? bestMatch.getCategory().getName() : null)
                            .score(similarity)
                            .source("springai-milvus")
                            .build());
                }
            }

            log.info("[SpringAI RAG] No match above threshold, generating with LLM");
            String generated = llmService.generateAnswerWithContext(query, buildContext(matches));
            return RAGResult.generated(generated, 0.5);

        } catch (Exception e) {
            log.error("[SpringAI RAG] RAG failed for query: {}", query, e);
            String fallback = llmService.generateAnswer(query);
            return RAGResult.generated(fallback, 0.3);
        }
    }

    private String buildContext(List<QAPairEntity> matches) {
        if (matches == null || matches.isEmpty()) {
            return "";
        }

        StringBuilder context = new StringBuilder();
        context.append("参考信息：\n");

        for (int i = 0; i < matches.size(); i++) {
            QAPairEntity match = matches.get(i);
            context.append(String.format("%d. 问题：%s\n   回答：%s\n",
                    i + 1, match.getQuestion(), match.getAnswer()));
        }

        return context.toString();
    }

    public List<ScoredMatch> retrieve(String query, int topK) {
        List<QAPairEntity> matches = vectorStore.searchSimilar(query, topK);
        List<ScoredMatch> results = new ArrayList<>();

        List<Float> queryVector = embeddingService.generateEmbedding(query);

        for (QAPairEntity match : matches) {
            List<Float> matchVector = embeddingService.parseVectorJson(match.getVectorJson());
            double similarity = embeddingService.calculateSimilarity(queryVector, matchVector);

            results.add(ScoredMatch.builder()
                    .id(match.getId().toString())
                    .question(match.getQuestion())
                    .answer(match.getAnswer())
                    .category(match.getCategory() != null ? match.getCategory().getName() : null)
                    .score(similarity)
                    .source("springai-milvus")
                    .build());
        }

        return results;
    }
}
