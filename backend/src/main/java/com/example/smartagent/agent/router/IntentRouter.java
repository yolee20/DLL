package com.example.smartagent.agent.router;

import com.example.smartagent.model.strategy.ModelRequest;
import com.example.smartagent.model.strategy.ModelResponse;
import com.example.smartagent.service.embedding.EmbeddingService;
import com.example.smartagent.service.llm.ModelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class IntentRouter {

    private final ModelService modelService;
    private final EmbeddingService embeddingService;

    @Value("${agent.intent-threshold:0.7}")
    private double intentThreshold;

    @Value("${agent.intent-model-name:intent_model}")
    private String intentModelName;

    private static final Map<String, String> INTENT_PATTERNS = Map.of(
            "qa", "qa",
            "question", "qa",
            "help", "qa",
            "询问", "qa",
            "聊天", "qa",
            "order", "order",
            "订单", "order",
            "weather", "weather",
            "天气", "weather"
    );

    public IntentResult route(String userMessage, Map<String, Object> context) {
        log.info("Routing intent for message: {}", userMessage);

        try {
            ModelResponse response = modelService.callModel(intentModelName, ModelRequest.of(userMessage));
            String intent = response.getOutput();
            double confidence = response.getConfidence();

            if (confidence < intentThreshold) {
                intent = matchByRules(userMessage);
                confidence = intent != null ? 0.6 : 0.0;
            }

            log.info("Intent routed: {} with confidence: {}", intent, confidence);
            return new IntentResult(intent != null ? intent : "unknown", confidence);

        } catch (Exception e) {
            log.warn("Model call failed, falling back to rule-based matching", e);
            String intent = matchByRules(userMessage);
            return new IntentResult(intent != null ? intent : "unknown", 0.5);
        }
    }

    private String matchByRules(String message) {
        String lowerMessage = message.toLowerCase();
        for (Map.Entry<String, String> entry : INTENT_PATTERNS.entrySet()) {
            if (lowerMessage.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    public Set<String> getSupportedIntents() {
        return Set.of("qa", "order", "weather", "unknown");
    }

    public record IntentResult(String intent, double confidence) {}
}
