package com.example.smartagent.mcp.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

@Component
@Slf4j
public class IntentRecognitionStrategy implements ModelStrategy {

    private static final Map<String, Double> DEFAULT_INTENTS = Map.of(
            "qa", 0.7,
            "order", 0.7,
            "weather", 0.7
    );

    private static final Set<String> QA_KEYWORDS = Set.of("怎么", "如何", "帮助", "什么", "为什么", "?");
    private static final Set<String> ORDER_KEYWORDS = Set.of("订单", "物流", "发货", "快递", "收货");
    private static final Set<String> WEATHER_KEYWORDS = Set.of("天气", "温度", "预报", "气候");

    @Override
    public String getModelType() {
        return "intent_model";
    }

    @Override
    public ModelOutput execute(String input) {
        log.debug("Executing intent recognition for input: {}", input);

        Map<String, Double> intentScores = calculateIntentScores(input);
        String bestIntent = findBestIntent(intentScores);
        double confidence = intentScores.getOrDefault(bestIntent, 0.5);

        return ModelOutput.success(bestIntent, confidence);
    }

    private Map<String, Double> calculateIntentScores(String input) {
        Map<String, Double> scores = new HashMap<>(DEFAULT_INTENTS);

        for (String keyword : QA_KEYWORDS) {
            if (input.contains(keyword)) {
                scores.merge("qa", 0.15, Double::sum);
            }
        }

        for (String keyword : ORDER_KEYWORDS) {
            if (input.contains(keyword)) {
                scores.merge("order", 0.20, Double::sum);
            }
        }

        for (String keyword : WEATHER_KEYWORDS) {
            if (input.contains(keyword)) {
                scores.merge("weather", 0.18, Double::sum);
            }
        }

        double maxScore = scores.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
        if (maxScore > 1.0) {
            scores.replaceAll((k, v) -> v / maxScore);
        }

        return scores;
    }

    private String findBestIntent(Map<String, Double> scores) {
        return scores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("qa");
    }
}