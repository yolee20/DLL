package com.example.smartagent.agent;

import com.example.smartagent.dto.request.ChatRequest;
import com.example.smartagent.dto.response.ChatResponse;
import com.example.smartagent.entity.AgentConversation;
import com.example.smartagent.repository.AgentConversationRepository;
import com.example.smartagent.skill.Skill;
import com.example.smartagent.skill.SkillResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SmartAgent 智能代理核心类
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SmartAgent {

    private final IntentParser intentParser;
    private final DialogueManager dialogueManager;
    private final SkillRouter skillRouter;
    private final ResponseGenerator responseGenerator;
    private final AgentConversationRepository conversationRepository;
    private final ObjectMapper objectMapper;

    private final Map<String, Map<String, Object>> sessions = new ConcurrentHashMap<>();

    public ChatResponse chat(ChatRequest request) {
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString();
        }

        log.info("Agent chatting with session: {}, message: {}", sessionId, request.getMessage());

        Map<String, Object> context = dialogueManager.getContext(sessionId);
        if (request.getContext() != null) {
            context.putAll(request.getContext());
        }

        IntentParser.IntentResult intentResult = intentParser.parse(request.getMessage(), context);
        log.info("Intent recognized: {} with confidence: {}", intentResult.intent(), intentResult.confidence());

        Skill skill = skillRouter.route(intentResult.intent());
        log.info("Routing to skill: {}", skill.getName());

        SkillResult skillResult = skill.execute(request.getMessage(), context);
        log.info("Skill result: success={}, answer={}", skillResult.getSuccess(), skillResult.getAnswer());

        String response = responseGenerator.generateResponse(skillResult);

        context.put("turn", context.getOrDefault("turn", 0));
        dialogueManager.updateContext(sessionId, context);

        saveConversation(sessionId, request.getMessage(), intentResult, skill, response, context);

        return ChatResponse.builder()
                .response(response)
                .sessionId(sessionId)
                .intent(intentResult.intent())
                .intentConfidence(intentResult.confidence())
                .skillUsed(skill.getName())
                .context(context)
                .build();
    }

    private void saveConversation(String sessionId, String userMessage,
                                  IntentParser.IntentResult intentResult, Skill skill,
                                  String response, Map<String, Object> context) {
        AgentConversation conversation = new AgentConversation();
        conversation.setSessionId(sessionId);
        conversation.setUserMessage(userMessage);
        conversation.setIntent(intentResult.intent());
        conversation.setIntentConfidence(BigDecimal.valueOf(intentResult.confidence()));
        conversation.setSkillName(skill.getName());
        conversation.setAgentResponse(response);
        try {
            conversation.setContextJson(objectMapper.writeValueAsString(context));
        } catch (JsonProcessingException e) {
            log.error("序列化上下文失败", e);
        }

        conversationRepository.save(conversation);
    }

    public SessionContext getSession(String sessionId) {
        Map<String, Object> context = sessions.get(sessionId);
        if (context == null) {
            context = new HashMap<>();
        }
        return new SessionContext(sessionId, context);
    }

    public void endSession(String sessionId) {
        sessions.remove(sessionId);
        log.info("Session ended: {}", sessionId);
    }

    public record SessionContext(String sessionId, Map<String, Object> context) {}
}