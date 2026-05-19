package com.example.smartagent.service;

import com.example.smartagent.entity.QAPair;
import com.example.smartagent.repository.QAPairRepository;
import io.milvus.client.MilvusClient;
import io.milvus.grpc.DataType;
import io.milvus.grpc.SearchResults;
import io.milvus.param.*;
import io.milvus.param.collection.*;
import io.milvus.param.dml.DeleteParam;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.response.SearchResultsWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Primary
@Service("milvusVectorStore")
@RequiredArgsConstructor
@Slf4j
public class MilvusVectorStore implements VectorStore {

    private static final String COLLECTION_NAME = "qa_embeddings";
    private static final int DIMENSION = 1536;

    private final MilvusClient milvusClient;
    private final EmbeddingService embeddingService;
    private final QAPairRepository qaPairRepository;

    @Value("${milvus.enabled:true}")
    private boolean milvusEnabled;

    private boolean milvusAvailable = false;

    @PostConstruct
    public void init() {
        if (milvusEnabled) {
            try {
                createCollectionIfNotExists();
                milvusAvailable = true;
                log.info("Milvus vector store initialized successfully");
            } catch (Exception e) {
                log.warn("Milvus not available, will use fallback storage: {}", e.getMessage());
                milvusAvailable = false;
            }
        }
    }

    private void createCollectionIfNotExists() {
        try {
            milvusClient.describeCollection(DescribeCollectionParam.newBuilder()
                    .withCollectionName(COLLECTION_NAME)
                    .build());
            log.info("Collection {} already exists", COLLECTION_NAME);
        } catch (Exception e) {
            log.info("Creating collection {}", COLLECTION_NAME);

            FieldType idField = FieldType.newBuilder()
                    .withName("id")
                    .withDataType(DataType.Int64)
                    .withPrimaryKey(true)
                    .withAutoID(false)
                    .build();

            FieldType embeddingField = FieldType.newBuilder()
                    .withName("embedding")
                    .withDataType(DataType.FloatVector)
                    .withDimension(DIMENSION)
                    .build();

            CollectionSchemaParam schema = CollectionSchemaParam.newBuilder()
                    .withFieldTypes(Arrays.asList(idField, embeddingField))
                    .build();

            CreateCollectionParam createParam = CreateCollectionParam.newBuilder()
                    .withCollectionName(COLLECTION_NAME)
                    .withDescription("my collection")
                    .withSchema(schema)   // ← 新推荐方式
                    .build();

            milvusClient.createCollection(createParam);

            CreateIndexParam indexParam = CreateIndexParam.newBuilder()
                    .withCollectionName(COLLECTION_NAME)
                    .withFieldName("embedding")
                    .withIndexType(io.milvus.param.IndexType.HNSW)
                    .withMetricType(io.milvus.param.MetricType.COSINE)
                    .withExtraParam("{\"M\": 16, \"efConstruction\": 200}")
                    .build();

            milvusClient.createIndex(indexParam);

            milvusClient.loadCollection(LoadCollectionParam.newBuilder()
                    .withCollectionName(COLLECTION_NAME)
                    .build());
        }
    }

    @Override
    public void storeVector(Long qaId, String text) {
        if (!milvusAvailable) {
            log.warn("Milvus not available, skipping vector storage");
            return;
        }

        try {
            // 1. 假设 embeddingService.generateEmbedding(text) 返回 List<Float>
            List<Float> embedding = embeddingService.generateEmbedding(text);


            // 2. 构造 Field：使用构造函数，而不是Builder
            // 对于主键 "id" 字段，值需要包装为 List<Long>
            InsertParam.Field idField = new InsertParam.Field("id", List.of(qaId));
            // 对于向量 "embedding" 字段，值需要包装为 List<List<Float>>
            InsertParam.Field vectorField = new InsertParam.Field("embedding", List.of(embedding));

            // 3. 构造 InsertParam
            InsertParam insertParam = InsertParam.newBuilder()
                    .withCollectionName(COLLECTION_NAME)
                    .withFields(List.of(idField, vectorField)) // 传入Field列表
                    .build();


            // 4. 执行插入
            milvusClient.insert(insertParam);
            milvusClient.flush(FlushParam.newBuilder().withCollectionNames(List.of(COLLECTION_NAME)).build());
            log.debug("Stored vector for QA pair: {}", qaId);
        } catch (Exception e) {
            log.error("Failed to store vector: {}", e.getMessage());
        }
    }

    @Override
    public List<QAPair> searchSimilar(String query, int topK) {
        if (!milvusAvailable) {
            log.warn("Milvus not available, falling back to full table scan");
            return fallbackSearch(query, topK);
        }

        try {
            List<Float> queryEmbedding = embeddingService.generateEmbedding(query);

            SearchParam searchParam = SearchParam.newBuilder()
                    .withCollectionName(COLLECTION_NAME)
                    .withVectors(List.of(queryEmbedding))
                    .withTopK(topK)
                    .withMetricType(io.milvus.param.MetricType.COSINE)
                    .withParams("{\"ef\": 100}")
                    .build();

            R<SearchResults> results = milvusClient.search(searchParam);
            SearchResultsWrapper wrapper = new SearchResultsWrapper(results.getData().getResults());

            List<Long> qaIds = wrapper.getIDScore(0).stream()
                    .map(idScore -> idScore.getLongID())
                    .collect(Collectors.toList());

            return qaIds.stream()
                    .map(qaPairRepository::findById)
                    .filter(Objects::nonNull)
                    .filter(java.util.Optional::isPresent)
                    .map(java.util.Optional::get)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Milvus search failed, falling back: {}", e.getMessage());
            return fallbackSearch(query, topK);
        }
    }

    private List<QAPair> fallbackSearch(String query, int topK) {
        List<QAPair> allPairs = qaPairRepository.findAll();
        if (allPairs.isEmpty()) {
            return List.of();
        }

        List<Float> queryEmbedding = embeddingService.generateEmbedding(query);

        return allPairs.stream()
                .filter(p -> p.getVectorJson() != null)
                .map(p -> {
                    try {
                        List<Float> docEmbedding = embeddingService.parseVectorJson(p.getVectorJson());
                        double similarity = embeddingService.calculateSimilarity(queryEmbedding, docEmbedding);
                        return new Pair<>(p, similarity);
                    } catch (Exception e) {
                        return new Pair<>(p, 0.0);
                    }
                })
                .filter(p -> p.similarity > 0.3)
                .sorted((a, b) -> Double.compare(b.similarity, a.similarity))
                .limit(topK)
                .map(p -> p.pair)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteVector(Long qaId) {
        if (!milvusAvailable) {
            return;
        }
        try {
            milvusClient.delete(DeleteParam.newBuilder()
                    .withCollectionName(COLLECTION_NAME)
                    .withExpr("id == " + qaId)
                    .build());
        } catch (Exception e) {
            log.error("Failed to delete vector: {}", e.getMessage());
        }
    }

    private record Pair<QAPair, Double>(QAPair pair, Double similarity) {}
}