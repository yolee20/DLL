package com.example.smartagent.agent.thinker;

import com.example.smartagent.agent.core.AgentStateMachine;
import com.example.smartagent.agent.handler.ToolDecider;
import com.example.smartagent.agent.router.IntentRouter;
import com.example.smartagent.model.strategy.ModelRequest;
import com.example.smartagent.model.strategy.ModelResponse;
import com.example.smartagent.service.llm.ModelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReActThinker {

    private final ModelService modelService;
    private final IntentRouter intentRouter;
    private final ToolDecider toolDecider;

    @Value("${agent.think-max-iterations:3}")
    private int maxIterations;

    @Value("${agent.think-threshold:0.7}")
    private double thinkThreshold;

    public ThinkResult think(String sessionId, String userMessage,
                             Map<String, Object> context, AgentStateMachine stateMachine) {
        log.info("ReAct thinking for session: {}, iteration: 0", sessionId);

        int iteration = 0;
        String currentIntent = null;
        double confidence = 0.0;
        ToolDecider.ToolDecision toolDecision = null;

        while (iteration < maxIterations) {
            iteration++;
            log.debug("ReAct iteration {} for session: {}", iteration, sessionId);

            AgentStateMachine.State currentState = stateMachine.getState(sessionId);

            if (currentState == AgentStateMachine.State.THINK) {
                IntentRouter.IntentResult intentResult = intentRouter.route(userMessage, context);
                currentIntent = intentResult.intent();
                confidence = intentResult.confidence();

                log.info("Intent recognized: {} with confidence: {}", currentIntent, confidence);

                if (confidence >= thinkThreshold) {
                    toolDecision = toolDecider.decide(sessionId, userMessage, context, currentIntent);

                    stateMachine.transitionTo(sessionId, AgentStateMachine.State.EXECUTE);
                    return ThinkResult.builder()
                            .intent(currentIntent)
                            .confidence(confidence)
                            .needMoreAction(false)
                            .iteration(iteration)
                            .toolDecision(toolDecision)
                            .build();
                } else {
                    stateMachine.transitionTo(sessionId, AgentStateMachine.State.RETHINK);
                }
            } else if (currentState == AgentStateMachine.State.RETHINK) {
                String reasoning = generateReasoning(userMessage, context, currentIntent);
                context.put("reasoning", reasoning);

                if (shouldContinue(reasoning)) {
                    stateMachine.transitionTo(sessionId, AgentStateMachine.State.THINK);
                } else {
                    stateMachine.transitionTo(sessionId, AgentStateMachine.State.END);
                    return ThinkResult.builder()
                            .intent(currentIntent != null ? currentIntent : "unknown")
                            .confidence(0.5)
                            .needMoreAction(false)
                            .iteration(iteration)
                            .fallback(true)
                            .build();
                }
            } else if (currentState == AgentStateMachine.State.EXECUTE) {
                return ThinkResult.builder()
                        .intent(currentIntent)
                        .confidence(confidence)
                        .needMoreAction(false)
                        .iteration(iteration)
                        .toolDecision(toolDecision)
                        .build();
            }

            if (iteration >= maxIterations) {
                log.warn("Max iterations {} reached for session: {}", maxIterations, sessionId);
                stateMachine.transitionTo(sessionId, AgentStateMachine.State.END);
                return ThinkResult.builder()
                        .intent(currentIntent != null ? currentIntent : "unknown")
                        .confidence(0.5)
                        .needMoreAction(false)
                        .iteration(iteration)
                        .fallback(true)
                        .build();
            }
        }

        stateMachine.transitionTo(sessionId, AgentStateMachine.State.END);
        return ThinkResult.builder()
                .intent(currentIntent != null ? currentIntent : "unknown")
                .confidence(confidence)
                .needMoreAction(false)
                .iteration(iteration)
                .build();
    }

    private String generateReasoning(String userMessage, Map<String, Object> context, String currentIntent) {
        String prompt = String.format("""
            用户消息: %s
            当前意图: %s
            上下文: %s

            请分析用户意图是否正确，如果不正确请说明原因。
            """, userMessage, currentIntent, context);

        try {
            ModelResponse response = modelService.callModel("reasoning_model", ModelRequest.of(prompt));
            return response.getOutput();
        } catch (Exception e) {
            log.warn("Reasoning generation failed, using rule-based fallback", e);
            return "Rule-based reasoning";
        }
    }

    private boolean shouldContinue(String reasoning) {
        return reasoning != null &&
               reasoning.contains("继续") ||
               reasoning.contains("再") ||
               reasoning.contains("还");
    }

    @lombok.Builder
    @lombok.Data
    public static class ThinkResult {
        private String intent;
        private double confidence;
        private boolean needMoreAction;
        private int iteration;
        private boolean fallback;
        private ToolDecider.ToolDecision toolDecision;
    }
}
