package com.example.smartagent.service.springai;

import com.example.smartagent.entity.QAPairEntity;
import com.example.smartagent.service.vector.VectorStore;
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
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SpringAIVectorStore implements VectorStore {

    private final MilvusClient milvusClient;
    private final SpringAIEmbeddingService embeddingService;

    @Value("${milvus.host:localhost}")
    private String milvusHost;

    @Value("${milvus.port:19530}")
    private int milvusPort;

    @Value("${milvus.enabled:true}")
    private boolean milvusEnabled;

    private static final String COLLECTION_NAME = "qa_vectors_springai";
    private static final int DIMENSION = 1536;

    public SpringAIVectorStore(MilvusClient milvusClient, SpringAIEmbeddingService embeddingService) {
        this.milvusClient = milvusClient;
        this.embeddingService = embeddingService;
    }

    @PostConstruct
    public void init() {
        if (!milvusEnabled) {
            log.info("Milvus is disabled, SpringAIVectorStore will use in-memory mode");
            return;
        }

        try {
            createCollectionIfNotExists();
            log.info("SpringAI VectorStore initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize SpringAIVectorStore", e);
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
                    .withDescription("SpringAI QA collection")
                    .withSchema(schema)
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

            log.info("Created Milvus collection: {}", COLLECTION_NAME);
        }
    }

    @Override
    public void storeVector(Long qaId, String text) {
        try {
            List<Float> embedding = embeddingService.generateEmbedding(text);
            if (embedding.isEmpty()) {
                log.warn("Empty embedding generated for QA id: {}", qaId);
                return;
            }

            InsertParam.Field idField = new InsertParam.Field("id", List.of(qaId));
            InsertParam.Field vectorField = new InsertParam.Field("embedding", List.of(embedding));

            InsertParam insertParam = InsertParam.newBuilder()
                    .withCollectionName(COLLECTION_NAME)
                    .withFields(List.of(idField, vectorField))
                    .build();

            milvusClient.insert(insertParam);
            milvusClient.flush(FlushParam.newBuilder().withCollectionNames(List.of(COLLECTION_NAME)).build());

            log.debug("Stored vector for QA id: {}", qaId);
        } catch (Exception e) {
            log.error("Failed to store vector for QA id: {}", qaId, e);
        }
    }

    @Override
    public List<QAPairEntity> searchSimilar(String query, int topK) {
        try {
            List<Float> queryEmbedding = embeddingService.generateEmbedding(query);
            if (queryEmbedding.isEmpty()) {
                return List.of();
            }

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
                    .map(id -> {
                        QAPairEntity qa = new QAPairEntity();
                        qa.setId(id);
                        return qa;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Search failed", e);
            return List.of();
        }
    }

    @Override
    public void deleteVector(Long qaId) {
        try {
            milvusClient.delete(DeleteParam.newBuilder()
                    .withCollectionName(COLLECTION_NAME)
                    .withExpr("id == " + qaId)
                    .build());

            log.debug("Deleted vector for QA id: {}", qaId);
        } catch (Exception e) {
            log.error("Failed to delete vector for QA id: {}", qaId, e);
        }
    }
}
