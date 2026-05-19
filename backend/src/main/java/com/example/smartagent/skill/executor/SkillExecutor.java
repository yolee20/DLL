package com.example.smartagent.skill.executor;

import com.example.smartagent.skill.SkillResult;

import java.util.Map;

public interface SkillExecutor {

    String getExecutorType();

    SkillResult execute(String userMessage, Map<String, Object> context);

    boolean supports(String intent);
}