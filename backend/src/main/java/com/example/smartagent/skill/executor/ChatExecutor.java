package com.example.smartagent.skill.executor;

import com.example.smartagent.service.llm.LlmService;
import com.example.smartagent.skill.SkillResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatExecutor implements SkillExecutor {

    private final LlmService llmService;

    @Override
    public String getExecutorType() {
        return "chat";
    }

    @Override
    public SkillResult execute(String userMessage, Map<String, Object> context) {
        try {
            String answer = llmService.generateAnswer(userMessage);
            return SkillResult.success(answer, "ChatExecutor", 0.85);
        } catch (Exception e) {
            log.error("Chat execution failed", e);
            return SkillResult.failure("对话服务暂时不可用");
        }
    }

    @Override
    public boolean supports(String intent) {
        return true;
    }
}