package com.example.smartagent.skill;

import com.example.smartagent.skill.executor.RAGExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component("qaSkill")
@RequiredArgsConstructor
@Slf4j
@SkillDescriptor(
        name = "QASkill",
        description = "智能问答技能，支持RAG检索增强和LLM生成",
        category = "ai",
        version = "2.0.0"
)
public class QASkill extends AbstractSkill {

    private final RAGExecutor ragExecutor;

    @Override
    public String getName() {
        return "QASkill";
    }

    @Override
    protected SkillResult doExecute(String userMessage, Map<String, Object> context) {
        log.info("QASkill executing with message: {}", userMessage);
        return ragExecutor.execute(userMessage, context);
    }

    @Override
    protected Set<String> getDefaultSupportedIntents() {
        return Set.of("qa", "question", "help", "询问", "聊天");
    }
}