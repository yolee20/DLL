package com.example.smartagent.mcp;

import java.util.List;

public interface ModelControlProtocol {

    ModelResponse callModel(String modelName, ModelRequest request);

    void loadModel(String modelName);

    void unloadModel(String modelName);

    ModelStatus getModelStatus(String modelName);

    List<ModelStatus> getAllModelStatus();

    boolean isModelLoaded(String modelName);

    void reloadModel(String modelName);

    interface ModelControlProtocolFactory {
        ModelControlProtocol getInstance();
    }
}