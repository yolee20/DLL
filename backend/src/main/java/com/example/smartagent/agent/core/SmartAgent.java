package com.example.smartagent.agent.core;

import com.example.smartagent.agent.handler.ErrorHandler;
import com.example.smartagent.agent.handler.ResponseGenerator;
import com.example.smartagent.agent.handler.ToolDecider;
import com.example.smartagent.agent.registry.PluginRegistry;
import com.example.smartagent.agent.thinker.ReActThinker;
import com.example.smartagent.dto.request.ChatRequest;
import com.example.smartagent.dto.response.ChatResponse;
import com.example.smartagent.entity.AgentConversationEntity;
import com.example.smartagent.memory.DialogueManager;
import com.example.smartagent.repository.AgentConversationRepository;
import com.example.smartagent.skill.Skill;
import com.example.smartagent.skill.SkillResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
@Slf4j
public class SmartAgent {

    private final AgentStateMachine stateMachine;
    private final ReActThinker reactThinker;
    private final ErrorHandler errorHandler;
    private final PluginRegistry pluginRegistry;
    private final DialogueManager dialogueManager;
    private final ResponseGenerator responseGenerator;
    private final AgentConversationRepository conversationRepository;
    private final ObjectMapper objectMapper;
    private final ToolDecider toolDecider;

    public ChatResponse chat(ChatRequest request) {
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString();
        }

        log.info("SmartAgent processing chat for session: {}, message: {}", sessionId, request.getMessage());

        stateMachine.init(sessionId);
        errorHandler.init(sessionId);

        try {
            Map<String, Object> context = dialogueManager.getContext(sessionId);
            if (request.getContext() != null) {
                context.putAll(request.getContext());
            }

            ReActThinker.ThinkResult thinkResult = reactThinker.think(
                    sessionId, request.getMessage(), context, stateMachine);

            log.info("Think result: intent={}, confidence={}, iteration={}",
                    thinkResult.getIntent(), thinkResult.getConfidence(), thinkResult.getIteration());

            String intent = thinkResult.getIntent();
            Skill skill = pluginRegistry.get(intent);
            if (skill == null) {
                skill = pluginRegistry.get("QASkill");
                log.warn("Skill not found for intent: {}, using default QASkill", intent);
            }

            final Skill finalSkill = skill;
            SkillResult skillResult = errorHandler.catchError(sessionId, "skill.execute", () ->
                    finalSkill.execute(request.getMessage(), context));

            String response = responseGenerator.generateResponse(skillResult);

            Integer turn = (Integer) context.getOrDefault("turn", 0);
            context.put("turn", turn + 1);
            dialogueManager.updateContext(sessionId, context);

            saveConversation(sessionId, request.getMessage(), thinkResult, finalSkill, response, context);

            stateMachine.end(sessionId);

            return ChatResponse.builder()
                    .response(response)
                    .sessionId(sessionId)
                    .intent(thinkResult.getIntent())
                    .intentConfidence(thinkResult.getConfidence())
                    .skillUsed(finalSkill.getName())
                    .context(context)
                    .build();

        } catch (Exception e) {
            log.error("Chat processing failed for session: {}", sessionId, e);
            stateMachine.error(sessionId);
            errorHandler.getContext(sessionId).setLastException(e);
            return buildErrorResponse(sessionId, e);
        } finally {
            errorHandler.clear(sessionId);
        }
    }

    public Flux<StreamEvent> chatStream(ChatRequest request) {
        final String sessionId = request.getSessionId() != null && !request.getSessionId().isEmpty()
                ? request.getSessionId()
                : UUID.randomUUID().toString();

        log.info("SmartAgent processing stream chat for session: {}, message: {}",
                sessionId, request.getMessage());

        stateMachine.init(sessionId);
        errorHandler.init(sessionId);

        return Flux.create(sink -> {
            try {
                final Map<String, Object> context = dialogueManager.getContext(sessionId);
                if (request.getContext() != null) {
                    context.putAll(request.getContext());
                }

                final ReActThinker.ThinkResult thinkResult = reactThinker.think(
                        sessionId, request.getMessage(), context, stateMachine);

                log.info("Stream think result: intent={}, confidence={}",
                        thinkResult.getIntent(), thinkResult.getConfidence());

                String intent = thinkResult.getIntent();
                Skill skill = pluginRegistry.get(intent);
                if (skill == null) {
                    skill = pluginRegistry.get("QASkill");
                    log.warn("Skill not found for intent: {}, using default QASkill", intent);
                }

                final Skill finalSkill = skill;
                sink.next(StreamEvent.intent(thinkResult.getIntent(), thinkResult.getConfidence(), finalSkill.getName()));

                SkillResult skillResult = errorHandler.catchError(sessionId, "skill.execute", () ->
                        finalSkill.execute(request.getMessage(), context));

                final String response = responseGenerator.generateResponse(skillResult);

                AtomicInteger index = new AtomicInteger(0);
                int chunkSize = 10;
                while (index.get() < response.length()) {
                    int end = Math.min(index.get() + chunkSize, response.length());
                    String chunk = response.substring(index.get(), end);
                    sink.next(StreamEvent.chunk(chunk));
                    index.set(end);

                    try {
                        Thread.sleep(30);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }

                Integer turn = (Integer) context.getOrDefault("turn", 0);
                context.put("turn", turn + 1);
                dialogueManager.updateContext(sessionId, context);

                saveConversation(sessionId, request.getMessage(), thinkResult, finalSkill, response, context);

                stateMachine.end(sessionId);
                sink.next(StreamEvent.complete(sessionId));
                sink.complete();

            } catch (Exception e) {
                log.error("Stream chat processing failed for session: {}", sessionId, e);
                stateMachine.error(sessionId);
                sink.next(StreamEvent.error(e.getMessage()));
                sink.complete();
            } finally {
                errorHandler.clear(sessionId);
            }
        });
    }

    private void saveConversation(String sessionId, String userMessage,
                                  ReActThinker.ThinkResult thinkResult, Skill skill,
                                  String response, Map<String, Object> context) {
        try {
            AgentConversationEntity conversation = new AgentConversationEntity();
            conversation.setSessionId(sessionId);
            conversation.setUserMessage(userMessage);
            conversation.setIntent(thinkResult.getIntent());
            conversation.setIntentConfidence(BigDecimal.valueOf(thinkResult.getConfidence()));
            conversation.setSkillName(skill.getName());
            conversation.setAgentResponse(response);
            conversation.setContextJson(objectMapper.writeValueAsString(context));
            conversationRepository.save(conversation);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize context for session: {}", sessionId, e);
        }
    }

    private ChatResponse buildErrorResponse(String sessionId, Exception e) {
        String fallbackMessage = "抱歉，服务处理过程中遇到问题，请稍后重试。";

        if (errorHandler.isDegraded(sessionId)) {
            fallbackMessage = "服务处于降级模式，部分功能可能不可用。";
        }

        return ChatResponse.builder()
                .response(fallbackMessage)
                .sessionId(sessionId)
                .intent("error")
                .intentConfidence(0.0)
                .skillUsed("error")
                .build();
    }

    public AgentStateMachine.State getState(String sessionId) {
        return stateMachine.getState(sessionId);
    }

    public String getStateTrace(String sessionId) {
        return stateMachine.getTrace(sessionId);
    }

    public void endSession(String sessionId) {
        stateMachine.end(sessionId);
        dialogueManager.clearContext(sessionId);
        errorHandler.clear(sessionId);
        log.info("Session ended: {}", sessionId);
    }

    public record StreamEvent(String type, String content, String intent,
                             Double confidence, String skillUsed, String sessionId) {

        public static StreamEvent intent(String intent, Double confidence, String skillUsed) {
            return new StreamEvent("intent", null, intent, confidence, skillUsed, null);
        }

        public static StreamEvent chunk(String content) {
            return new StreamEvent("chunk", content, null, null, null, null);
        }

        public static StreamEvent complete(String sessionId) {
            return new StreamEvent("complete", null, null, null, null, sessionId);
        }

        public static StreamEvent error(String message) {
            return new StreamEvent("error", message, null, null, null, null);
        }
    }
}
