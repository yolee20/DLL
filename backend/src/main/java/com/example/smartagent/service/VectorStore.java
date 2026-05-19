package com.example.smartagent.service;

import com.example.smartagent.entity.QAPair;

import java.util.List;

public interface VectorStore {

    void storeVector(Long qaId, String text);

    List<QAPair> searchSimilar(String query, int topK);

    void deleteVector(Long qaId);
}