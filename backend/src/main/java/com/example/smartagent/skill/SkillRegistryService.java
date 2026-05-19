
package com.example.smartagent.skill;

import com.example.smartagent.entity.SkillRegistry;
import com.example.smartagent.repository.SkillRegistryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SkillRegistryService {
    private final SkillRegistryRepository skillRegistryRepository;
    private final ObjectMapper objectMapper;
    
    public List<SkillRegistry> getAllSkills() {
        return skillRegistryRepository.findAll();
    }
    
    public SkillRegistry getSkillByName(String name) {
        return skillRegistryRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("技能不存在: " + name));
    }
    
    public SkillRegistry saveSkill(SkillRegistry skill) {
        return skillRegistryRepository.save(skill);
    }
    
    public void deleteSkill(String name) {
        SkillRegistry skill = getSkillByName(name);
        skillRegistryRepository.delete(skill);
    }
    
    public SkillRegistry enableSkill(String name) {
        SkillRegistry skill = getSkillByName(name);
        skill.setEnabled(true);
        return skillRegistryRepository.save(skill);
    }
    
    public SkillRegistry disableSkill(String name) {
        SkillRegistry skill = getSkillByName(name);
        skill.setEnabled(false);
        return skillRegistryRepository.save(skill);
    }
    
    public Set<String> getIntentPatterns(String skillName) {
        SkillRegistry skill = getSkillByName(skillName);
        try {
            return objectMapper.readValue(skill.getIntentPatterns(), 
                    new TypeReference<Set<String>>() {});
        } catch (JsonProcessingException e) {
            log.error("解析意图模式失败", e);
            return Set.of();
        }
    }
    
    public List<SkillRegistry> getEnabledSkills() {
        return skillRegistryRepository.findByEnabled(true);
    }
}
