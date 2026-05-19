package com.example.smartagent.service.rag;

import com.example.smartagent.entity.QAPairEntity;
import com.example.smartagent.service.embedding.EmbeddingService;
import com.example.smartagent.service.vector.VectorStore;
import com.example.smartagent.service.llm.LlmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RAGEngine {

    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;
    private final LlmService llmService;
    private final RagPromptTemplate ragPromptTemplate;
    private final ExecutorService executorService = Executors.newFixedThreadPool(4);

    @Value("${rag.threshold:0.75}")
    private double similarityThreshold;

    @Value("${rag.top-k:5}")
    private int topK;

    private final List<RetrievalSource> retrievalSources = new ArrayList<>();

    public void addRetrievalSource(RetrievalSource source) {
        retrievalSources.add(source);
        log.info("Added retrieval source: {}", source.getName());
    }

    public RAGResult retrieveOrGenerate(String query) {
        return retrieveOrGenerate(query, topK, similarityThreshold);
    }

    public RAGResult retrieveOrGenerate(String query, int topK, double threshold) {
        log.info("RAG query: {}, topK: {}, threshold: {}", query, topK, threshold);

        try {
            List<ScoredMatch> allMatches = multiSourceRetrieve(query, topK);
            List<ScoredMatch> mergedMatches = mergeAndRank(allMatches, topK);

            if (!mergedMatches.isEmpty()) {
                ScoredMatch bestMatch = mergedMatches.get(0);

                log.info("Found {} matches, best score: {}, source: {}",
                        mergedMatches.size(), bestMatch.getScore(), bestMatch.getSource());

                if (bestMatch.getScore() >= threshold) {
                    return RAGResult.fromMatch(bestMatch);
                }
            }

            log.info("No match above threshold, generating with LLM");
            String fullPrompt = ragPromptTemplate.buildRagPrompt(query, mergedMatches);
            String generated = llmService.generateAnswer(fullPrompt);
            return RAGResult.generated(generated, 0.5);

        } catch (Exception e) {
            log.error("RAG failed for query: {}", query, e);
            String fallback = llmService.generateAnswer(query);
            return RAGResult.generated(fallback, 0.3);
        }
    }

    private List<ScoredMatch> multiSourceRetrieve(String query, int topK) {
        List<Future<List<ScoredMatch>>> futures = new ArrayList<>();

        for (RetrievalSource source : retrievalSources) {
            futures.add(executorService.submit(() -> source.retrieve(query, topK)));
        }

        futures.add(executorService.submit(() -> retrieveFromVectorStore(query, topK)));

        List<ScoredMatch> allMatches = new ArrayList<>();
        for (Future<List<ScoredMatch>> future : futures) {
            try {
                allMatches.addAll(future.get(5, TimeUnit.SECONDS));
            } catch (Exception e) {
                log.warn("Retrieval source failed", e);
            }
        }

        return allMatches;
    }

    private List<ScoredMatch> retrieveFromVectorStore(String query, int topK) {
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
                    .source("milvus")
                    .build());
        }

        return results;
    }

    private List<ScoredMatch> mergeAndRank(List<ScoredMatch> matches, int topK) {
        return matches.stream()
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(topK)
                .collect(Collectors.toList());
    }

    public List<ScoredMatch> retrieve(String query, int topK) {
        return multiSourceRetrieve(query, topK);
    }

    public interface RetrievalSource {
        String getName();
        List<ScoredMatch> retrieve(String query, int topK);
    }

    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
