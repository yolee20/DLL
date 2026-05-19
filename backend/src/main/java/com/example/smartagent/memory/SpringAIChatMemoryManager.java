
package com.example.smartagent.memory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于Spring AI的对话记忆管理器
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SpringAIChatMemoryManager {

    private final ChatClient chatClient;

    private final Map<String, List<Message>> sessionMemory = new ConcurrentHashMap<>();

    public String initSession() {
        String sessionId = UUID.randomUUID().toString();
        sessionMemory.put(sessionId, new ArrayList<>());
        log.info("Spring AI ChatMemory: Init session {}", sessionId);
        return sessionId;
    }

    public void addUserMessage(String sessionId, String content) {
        List<Message> messages = getOrCreateMessages(sessionId);
        messages.add(new UserMessage(content));
        log.debug("Spring AI ChatMemory: User message added to {}", sessionId);
    }

    public void addAssistantMessage(String sessionId, String content) {
        List<Message> messages = getOrCreateMessages(sessionId);
        messages.add(new AssistantMessage(content));
        log.debug("Spring AI ChatMemory: Assistant message added to {}", sessionId);
    }

    public String chatWithMemory(String sessionId, String userMessage) {
        log.info("Spring AI ChatMemory: Session {} chat", sessionId);

        List<Message> messages = getOrCreateMessages(sessionId);
        messages.add(new UserMessage(userMessage));

        Prompt prompt = new Prompt(new ArrayList<>(messages));
        String response = chatClient.prompt(prompt).call().content();

        messages.add(new AssistantMessage(response));

        log.info("Spring AI ChatMemory: Session {} chat complete", sessionId);
        return response;
    }

    public List<Message> getHistory(String sessionId) {
        return new ArrayList<>(getOrCreateMessages(sessionId));
    }

    public void clearSession(String sessionId) {
        sessionMemory.remove(sessionId);
        log.info("Spring AI ChatMemory: Clear session {}", sessionId);
    }

    private List<Message> getOrCreateMessages(String sessionId) {
        return sessionMemory.computeIfAbsent(sessionId, k -> new ArrayList<>());
    }
}
