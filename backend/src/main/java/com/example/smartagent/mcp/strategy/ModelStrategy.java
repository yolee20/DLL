package com.example.smartagent.mcp.strategy;

public interface ModelStrategy {

    String getModelType();

    ModelOutput execute(String input);

    record ModelOutput(String output, double confidence) {
        public static ModelOutput success(String output) {
            return new ModelOutput(output, 1.0);
        }

        public static ModelOutput success(String output, double confidence) {
            return new ModelOutput(output, confidence);
        }
    }
}