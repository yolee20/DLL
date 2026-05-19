package com.example.smartagent.service.conversation;

import com.example.smartagent.dto.response.ConversationResponse;
import com.example.smartagent.dto.response.SessionResponse;
import com.example.smartagent.entity.AgentConversationEntity;
import com.example.smartagent.memory.SessionManager;
import com.example.smartagent.repository.AgentConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationService {

    private final AgentConversationRepository conversationRepository;
    private final SessionManager sessionManager;

    public List<String> getAllSessions() {
        return conversationRepository.findAll().stream()
                .map(AgentConversationEntity::getSessionId)
                .distinct()
                .collect(Collectors.toList());
    }

    public SessionResponse getSession(String sessionId) {
        List<AgentConversationEntity> conversations = conversationRepository
                .findBySessionIdOrderByCreatedAtAsc(sessionId);

        List<ConversationResponse> messages = conversations.stream()
                .map(c -> ConversationResponse.builder()
                        .id(c.getId())
                        .sessionId(c.getSessionId())
                        .userMessage(c.getUserMessage())
                        .intent(c.getIntent())
                        .intentConfidence(c.getIntentConfidence() != null ? c.getIntentConfidence().doubleValue() : null)
                        .skillName(c.getSkillName())
                        .agentResponse(c.getAgentResponse())
                        .createdAt(c.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return SessionResponse.builder()
                .sessionId(sessionId)
                .messages(messages)
                .createdAt(messages.isEmpty() ? null : messages.get(0).getCreatedAt())
                .build();
    }

    public List<AgentConversationEntity> getConversationHistory(String sessionId) {
        return conversationRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }

    public void deleteSession(String sessionId) {
        List<AgentConversationEntity> conversations = conversationRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        conversationRepository.deleteAll(conversations);
        sessionManager.clearHistory(sessionId);
        log.info("Session deleted: {}", sessionId);
    }

    public long countConversations() {
        return conversationRepository.count();
    }

    public long countBySkillName(String skillName) {
        return conversationRepository.countBySkillName(skillName);
    }
}
