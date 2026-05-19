package com.example.smartagent.skill;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;

@Slf4j
public abstract class AbstractSkill implements Skill {

    protected static final double DEFAULT_CONFIDENCE = 0.8;
    protected static final double HIGH_CONFIDENCE = 0.9;

    @Override
    public final SkillResult execute(String userMessage, Map<String, Object> context) {
        long startTime = System.currentTimeMillis();

        preExecute(userMessage, context);

        SkillResult result = doExecute(userMessage, context);

        postExecute(userMessage, context, result);

        long duration = System.currentTimeMillis() - startTime;
        log.info("Skill {} executed in {}ms, success: {}, confidence: {}",
                getName(), duration, result.getSuccess(), result.getConfidence());

        return result;
    }

    protected void preExecute(String userMessage, Map<String, Object> context) {
        log.debug("Pre-execute skill: {} with message: {}", getName(), userMessage);
    }

    protected abstract SkillResult doExecute(String userMessage, Map<String, Object> context);

    protected void postExecute(String userMessage, Map<String, Object> context, SkillResult result) {
        log.debug("Post-execute skill: {} with result: {}", getName(), result.getSuccess());
    }

    protected SkillResult success(String answer) {
        return SkillResult.success(answer, getName(), DEFAULT_CONFIDENCE);
    }

    protected SkillResult success(String answer, double confidence) {
        return SkillResult.success(answer, getName(), confidence);
    }

    protected SkillResult failure(String message) {
        return SkillResult.failure(message);
    }

    @Override
    public Set<String> getSupportedIntents() {
        return getDefaultSupportedIntents();
    }

    protected abstract Set<String> getDefaultSupportedIntents();

    public String getDescription() {
        return getClass().getAnnotation(SkillDescriptor.class).description();
    }

    public SkillMetadata getMetadata() {
        SkillDescriptor annotation = getClass().getAnnotation(SkillDescriptor.class);
        if (annotation == null) {
            return new SkillMetadata(getName(), "unknown", "unknown");
        }
        return new SkillMetadata(
                annotation.name(),
                annotation.description(),
                annotation.version()
        );
    }

    public record SkillMetadata(String name, String description, String version) {}
}