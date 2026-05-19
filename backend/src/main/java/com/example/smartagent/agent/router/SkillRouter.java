package com.example.smartagent.agent.router;

import com.example.smartagent.agent.registry.PluginRegistry;
import com.example.smartagent.skill.OrderSkill;
import com.example.smartagent.skill.QASkill;
import com.example.smartagent.skill.Skill;
import com.example.smartagent.skill.WeatherSkill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class SkillRouter {

    private final Map<String, List<SkillWithWeight>> intentSkillMap = new ConcurrentHashMap<>();
    private final Skill defaultSkill;
    private final PluginRegistry pluginRegistry;

    public SkillRouter(QASkill qaSkill, OrderSkill orderSkill, WeatherSkill weatherSkill, PluginRegistry pluginRegistry) {
        this.pluginRegistry = pluginRegistry;
        
        registerSkill(qaSkill, 1.0);
        registerSkill(orderSkill, 0.8);
        registerSkill(weatherSkill, 0.9);
        
        this.defaultSkill = qaSkill;
        
        log.info("SkillRouter initialized with {} intent-skill mappings", intentSkillMap.size());
    }

    public void registerSkill(Skill skill, double weight) {
        for (String intent : skill.getSupportedIntents()) {
            intentSkillMap.computeIfAbsent(intent, k -> new ArrayList<>())
                    .add(new SkillWithWeight(skill, weight));
            
            intentSkillMap.get(intent).sort((a, b) -> Double.compare(b.weight, a.weight));
        }
        log.info("Registered skill: {} with weight: {}", skill.getName(), weight);
    }

    public Skill route(String intent) {
        return route(intent, null);
    }

    public Skill route(String intent, Map<String, Object> context) {
        List<SkillWithWeight> candidates = intentSkillMap.get(intent);
        
        if (candidates != null && !candidates.isEmpty()) {
            Skill pluginSkill = pluginRegistry.get(intent);
            if (pluginSkill != null) {
                log.info("Using plugin skill for intent: {}", intent);
                return pluginSkill;
            }
            
            log.info("Using highest weight skill for intent: {}", intent);
            return candidates.get(0).skill;
        }
        
        log.warn("No skill found for intent: {}, using default skill", intent);
        return defaultSkill;
    }

    public Map<String, List<SkillWithWeight>> getIntentSkillMap() {
        return intentSkillMap;
    }

    public static class SkillWithWeight {
        public final Skill skill;
        public final double weight;

        public SkillWithWeight(Skill skill, double weight) {
            this.skill = skill;
            this.weight = weight;
        }
    }
}
