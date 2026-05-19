package com.example.smartagent.agent.handler;

import com.example.smartagent.agent.registry.PluginRegistry;
import com.example.smartagent.skill.Skill;
import com.example.smartagent.skill.SkillResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ToolDecider {

    private final PluginRegistry pluginRegistry;

    public ToolDecision decide(String sessionId, String userMessage,
                              Map<String, Object> context, String currentIntent) {
        log.info("Deciding tool usage for session: {}, intent: {}", sessionId, currentIntent);

        Skill skill = pluginRegistry.get(currentIntent);
        if (skill == null) {
            log.warn("No skill found for intent: {}, using default QASkill", currentIntent);
            skill = pluginRegistry.get("QASkill");
        }

        boolean needsTool = skill != null && isToolSkill(skill);

        return ToolDecision.builder()
                .needsTool(needsTool)
                .selectedSkill(skill)
                .skillName(skill != null ? skill.getName() : "QASkill")
                .build();
    }

    public SkillResult executeTool(String sessionId, Skill skill,
                                    String userMessage, Map<String, Object> context) {
        log.info("Executing tool for session: {}, skill: {}", sessionId, skill.getName());

        try {
            return skill.execute(userMessage, context);
        } catch (Exception e) {
            log.error("Tool execution failed for session: {}, skill: {}",
                    sessionId, skill.getName(), e);
            return SkillResult.failure("Tool execution failed: " + e.getMessage());
        }
    }

    private boolean isToolSkill(Skill skill) {
        String name = skill.getName().toLowerCase();
        return name.contains("tool") ||
               name.contains("api") ||
               name.contains("function") ||
               name.contains("call");
    }

    @lombok.Builder
    @lombok.Data
    public static class ToolDecision {
        private boolean needsTool;
        private Skill selectedSkill;
        private String skillName;
        private String toolName;
        private Map<String, Object> toolParams;
    }
}
