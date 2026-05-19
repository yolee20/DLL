package com.example.smartagent.agent.registry;

import com.example.smartagent.skill.Skill;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class PluginRegistry {

    private final List<Skill> skills;

    private final Map<String, PluginInfo> pluginRegistry = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        registerDefaultSkills();
        log.info("PluginRegistry initialized with {} plugins", pluginRegistry.size());
    }

    private void registerDefaultSkills() {
        for (Skill skill : skills) {
            PluginInfo info = PluginInfo.builder()
                    .name(skill.getName())
                    .skill(skill)
                    .enabled(true)
                    .weight(1.0)
                    .version(getSkillVersion(skill))
                    .build();
            pluginRegistry.put(skill.getName(), info);
            log.debug("Registered skill: {} with weight: {}", skill.getName(), info.getWeight());
        }
    }

    private String getSkillVersion(Skill skill) {
        try {
            var annotation = skill.getClass().getAnnotation(com.example.smartagent.skill.SkillDescriptor.class);
            return annotation != null ? annotation.version() : "1.0.0";
        } catch (Exception e) {
            return "1.0.0";
        }
    }

    public Skill get(String name) {
        PluginInfo info = pluginRegistry.get(name);
        if (info == null || !info.isEnabled()) {
            log.warn("Plugin not found or disabled: {}", name);
            return null;
        }
        return info.getSkill();
    }

    public Skill get(String name, double minWeight) {
        PluginInfo info = pluginRegistry.get(name);
        if (info == null || !info.isEnabled()) {
            log.warn("Plugin not found or disabled: {}", name);
            return null;
        }
        if (info.getWeight() < minWeight) {
            log.warn("Plugin {} weight {} below minimum {}", name, info.getWeight(), minWeight);
            return null;
        }
        return info.getSkill();
    }

    public List<Skill> getAll() {
        return pluginRegistry.values().stream()
                .filter(PluginInfo::isEnabled)
                .map(PluginInfo::getSkill)
                .collect(Collectors.toList());
    }

    public List<Skill> getAllSorted() {
        return pluginRegistry.values().stream()
                .filter(PluginInfo::isEnabled)
                .sorted(Comparator.comparingDouble(PluginInfo::getWeight).reversed())
                .map(PluginInfo::getSkill)
                .collect(Collectors.toList());
    }

    public void register(Skill skill, double weight) {
        PluginInfo info = PluginInfo.builder()
                .name(skill.getName())
                .skill(skill)
                .enabled(true)
                .weight(weight)
                .version(getSkillVersion(skill))
                .build();
        pluginRegistry.put(skill.getName(), info);
        log.info("Registered plugin: {} with weight: {}", skill.getName(), weight);
    }

    public void unregister(String name) {
        PluginInfo removed = pluginRegistry.remove(name);
        if (removed != null) {
            log.info("Unregistered plugin: {}", name);
        }
    }

    public void enable(String name) {
        PluginInfo info = pluginRegistry.get(name);
        if (info != null) {
            info.setEnabled(true);
            log.info("Enabled plugin: {}", name);
        }
    }

    public void disable(String name) {
        PluginInfo info = pluginRegistry.get(name);
        if (info != null) {
            info.setEnabled(false);
            log.info("Disabled plugin: {}", name);
        }
    }

    public void updateWeight(String name, double weight) {
        PluginInfo info = pluginRegistry.get(name);
        if (info != null) {
            info.setWeight(weight);
            log.info("Updated plugin {} weight to: {}", name, weight);
        }
    }

    public boolean isRegistered(String name) {
        return pluginRegistry.containsKey(name);
    }

    public boolean isEnabled(String name) {
        PluginInfo info = pluginRegistry.get(name);
        return info != null && info.isEnabled();
    }

    public double getWeight(String name) {
        PluginInfo info = pluginRegistry.get(name);
        return info != null ? info.getWeight() : 0.0;
    }

    public List<String> getPluginNames() {
        return pluginRegistry.values().stream()
                .filter(PluginInfo::isEnabled)
                .map(PluginInfo::getName)
                .collect(Collectors.toList());
    }

    public int getPluginCount() {
        return (int) pluginRegistry.values().stream().filter(PluginInfo::isEnabled).count();
    }

    public Map<String, PluginInfo> getPluginInfoMap() {
        return Map.copyOf(pluginRegistry);
    }

    @lombok.Builder
    @lombok.Data
    public static class PluginInfo {
        private String name;
        private Skill skill;
        private boolean enabled;
        private double weight;
        private String version;
        private String description;
    }
}
