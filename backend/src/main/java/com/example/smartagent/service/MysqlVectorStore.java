package com.example.smartagent.service;

import com.example.smartagent.entity.QAPair;
import com.example.smartagent.repository.QAPairRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service("mysqlVectorStore")
@RequiredArgsConstructor
@Slf4j
public class MysqlVectorStore implements VectorStore {

    private static final int TOP_K = 3;
    private static final double SIMILARITY_THRESHOLD = 0.3;

    private final QAPairRepository qaPairRepository;
    private final EmbeddingService embeddingService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void storeVector(Long qaId, String text) {
        try {
            List<Float> embedding = embeddingService.generateEmbedding(text);
            String vectorJson = objectMapper.writeValueAsString(embedding);
            
            qaPairRepository.findById(qaId).ifPresent(qaPair -> {
                qaPair.setVectorJson(vectorJson);
                qaPairRepository.save(qaPair);
                log.debug("Stored vector for QA pair: {}", qaId);
            });
        } catch (Exception e) {
            log.error("Failed to store vector: {}", e.getMessage());
        }
    }

    @Override
    public List<QAPair> searchSimilar(String query, int topK) {
        List<QAPair> allPairs = qaPairRepository.findAll();
        if (allPairs.isEmpty()) {
            return List.of();
        }

        try {
            List<Float> queryEmbedding = embeddingService.generateEmbedding(query);
            
            return allPairs.stream()
                    .filter(p -> p.getVectorJson() != null && !p.getVectorJson().isEmpty())
                    .map(p -> {
                        try {
                            List<Float> docEmbedding = objectMapper.readValue(
                                    p.getVectorJson(),
                                    new TypeReference<List<Float>>() {}
                            );
                            double similarity = embeddingService.calculateSimilarity(queryEmbedding, docEmbedding);
                            return new Pair<>(p, similarity);
                        } catch (JsonProcessingException e) {
                            return new Pair<>(p, 0.0);
                        }
                    })
                    .filter(p -> p.similarity > SIMILARITY_THRESHOLD)
                    .sorted((a, b) -> Double.compare(b.similarity, a.similarity))
                    .limit(topK)
                    .map(p -> p.pair)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Vector search failed: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public void deleteVector(Long qaId) {
        qaPairRepository.findById(qaId).ifPresent(qaPair -> {
            qaPair.setVectorJson(null);
            qaPairRepository.save(qaPair);
        });
    }

    private record Pair<QAPair, Double>(QAPair pair, Double similarity) {}
}