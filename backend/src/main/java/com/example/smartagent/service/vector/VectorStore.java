package com.example.smartagent.service.vector;

import com.example.smartagent.entity.QAPairEntity;

import java.util.List;

public interface VectorStore {

    void storeVector(Long qaId, String text);

    List<QAPairEntity> searchSimilar(String query, int topK);

    void deleteVector(Long qaId);
}