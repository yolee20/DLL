package com.example.smartagent.service.conversation;

import com.example.smartagent.memory.Message;
import com.example.smartagent.memory.SessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemoryService {

    private final SessionManager sessionManager;
    private final Map<String, Map<String, Object>> memoryStore = new ConcurrentHashMap<>();

    public void saveMemory(String sessionId, String key, Object value) {
        memoryStore.computeIfAbsent(sessionId, k -> new ConcurrentHashMap<>())
                .put(key, value);
        log.debug("Saved memory for session: {}, key: {}", sessionId, key);
    }

    public Object getMemory(String sessionId, String key) {
        Map<String, Object> sessionMemory = memoryStore.get(sessionId);
        return sessionMemory != null ? sessionMemory.get(key) : null;
    }

    public void clearMemory(String sessionId) {
        memoryStore.remove(sessionId);
        log.debug("Cleared memory for session: {}", sessionId);
    }

    public Map<String, Object> getAllMemory(String sessionId) {
        return new ConcurrentHashMap<>(memoryStore.getOrDefault(sessionId, new ConcurrentHashMap<>()));
    }

    public void addMessage(String sessionId, Message message) {
        sessionManager.addUserMessage(sessionId, message.getContent());
        log.debug("Added message to session: {}", sessionId);
    }

    public List<Message> getHistory(String sessionId) {
        return sessionManager.getHistory(sessionId);
    }
}