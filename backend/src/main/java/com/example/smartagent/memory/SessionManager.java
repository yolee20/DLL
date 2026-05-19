package com.example.smartagent.memory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class SessionManager {

    private static final String SESSION_PREFIX = "session:";
    private static final String HISTORY_PREFIX = "history:";

    private final StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper;

    @Value("${session.ttl-minutes:30}")
    private long ttlMinutes;

    @Value("${session.max-history:20}")
    private int maxHistory;

    public void addMessage(String sessionId, String role, String content) {
        String key = HISTORY_PREFIX + sessionId;
        Message message = Message.builder()
                .role(role)
                .content(content)
                .timestamp(System.currentTimeMillis())
                .build();

        try {
            String messageJson = objectMapper.writeValueAsString(message);
            redisTemplate.opsForList().leftPush(key, messageJson);
            redisTemplate.opsForList().trim(key, 0, maxHistory - 1);
            redisTemplate.expire(key, ttlMinutes, TimeUnit.MINUTES);

            log.debug("Added message to session: {}, role: {}", sessionId, role);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize message", e);
        }
    }

    public void addUserMessage(String sessionId, String content) {
        addMessage(sessionId, "user", content);
    }

    public void addAssistantMessage(String sessionId, String content) {
        addMessage(sessionId, "assistant", content);
    }

    public List<Message> getHistory(String sessionId) {
        String key = HISTORY_PREFIX + sessionId;
        List<String> messages = redisTemplate.opsForList().range(key, 0, -1);

        if (messages == null || messages.isEmpty()) {
            return new ArrayList<>();
        }

        List<Message> result = new ArrayList<>();
        for (String messageJson : messages) {
            try {
                Message message = objectMapper.readValue(messageJson, Message.class);
                result.add(message);
            } catch (JsonProcessingException e) {
                log.error("Failed to deserialize message", e);
            }
        }

        return result;
    }

    public void clearHistory(String sessionId) {
        String key = HISTORY_PREFIX + sessionId;
        redisTemplate.delete(key);
        log.info("Cleared history for session: {}", sessionId);
    }

    public boolean sessionExists(String sessionId) {
        String key = HISTORY_PREFIX + sessionId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public void setSessionAttribute(String sessionId, String attribute, String value) {
        String key = SESSION_PREFIX + sessionId + ":" + attribute;
        redisTemplate.opsForValue().set(key, value, ttlMinutes, TimeUnit.MINUTES);
    }

    public String getSessionAttribute(String sessionId, String attribute) {
        String key = SESSION_PREFIX + sessionId + ":" + attribute;
        return redisTemplate.opsForValue().get(key);
    }

    public long getActiveSessionCount() {
        return redisTemplate.keys(HISTORY_PREFIX + "*").size();
    }
}
