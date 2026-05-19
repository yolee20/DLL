
package com.example.smartagent.memory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 对话管理器（DialogueManager）
 * <p>
 * 负责管理对话上下文和会话状态，支持多轮对话。
 * 主要功能：
 * 1. 获取会话上下文
 * 2. 更新会话上下文
 * 3. 管理对话历史
 * </p>
 */
@Component
@Slf4j
public class DialogueManager {

    /** 会话上下文缓存 - 使用 ConcurrentHashMap 保证线程安全 */
    private final Map<String, Map<String, Object>> sessionContexts = new ConcurrentHashMap<>();

    /**
     * 获取会话上下文
     * <p>
     * 如果会话不存在，创建一个新的空上下文
     * </p>
     *
     * @param sessionId 会话ID
     * @return 会话上下文
     */
    public Map<String, Object> getContext(String sessionId) {
        return sessionContexts.computeIfAbsent(sessionId, k -> new HashMap<>());
    }

    /**
     * 更新会话上下文
     * <p>
     * 将新的上下文数据合并到现有上下文中
     * </p>
     *
     * @param sessionId 会话ID
     * @param context   新的上下文数据
     */
    public void updateContext(String sessionId, Map<String, Object> context) {
        Map<String, Object> existingContext = sessionContexts.computeIfAbsent(sessionId, k -> new HashMap<>());
        existingContext.putAll(context);
        log.debug("Context updated for session: {}", sessionId);
    }

    /**
     * 清除会话上下文
     * <p>
     * 从缓存中移除指定会话的上下文
     * </p>
     *
     * @param sessionId 会话ID
     */
    public void clearContext(String sessionId) {
        sessionContexts.remove(sessionId);
        log.info("Context cleared for session: {}", sessionId);
    }

    /**
     * 获取会话数量
     *
     * @return 当前活跃会话数量
     */
    public int getSessionCount() {
        return sessionContexts.size();
    }
}
