package com.example.smartagent.service.management;

import com.example.smartagent.dto.request.SkillRegisterRequest;
import com.example.smartagent.dto.request.SkillStatusRequest;
import com.example.smartagent.entity.SkillRegistryEntity;
import com.example.smartagent.exception.ResourceNotFoundException;
import com.example.smartagent.repository.SkillRegistryRepository;
import com.example.smartagent.skill.Skill;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class SkillManagementService {

    private final SkillRegistryRepository skillRegistryRepository;
    private final Map<String, Skill> skillMap;
    private final ObjectMapper objectMapper;

    public List<SkillRegistryEntity> getAllSkills() {
        return skillRegistryRepository.findAll();
    }

    public SkillRegistryEntity getSkill(String skillName) {
        return skillRegistryRepository.findByName(skillName)
                .orElseThrow(() -> new ResourceNotFoundException("技能", skillName));
    }

    @Transactional
    public SkillRegistryEntity createSkill(SkillRegisterRequest request) {
        String intentPatternsJson = serializeIntentPatterns(request.getIntentPatterns());

        SkillRegistryEntity skill = SkillRegistryEntity.builder()
                .name(request.getName())
                .description(request.getDescription())
                .intentPatterns(intentPatternsJson)
                .classPath(request.getClassPath())
                .enabled(true)
                .build();

        SkillRegistryEntity saved = skillRegistryRepository.save(skill);
        log.info("技能已创建: {}", saved.getName());
        return saved;
    }

    @Transactional
    public SkillRegistryEntity updateSkill(String skillName, SkillRegisterRequest request) {
        SkillRegistryEntity skill = skillRegistryRepository.findByName(skillName)
                .orElseThrow(() -> new ResourceNotFoundException("技能", skillName));

        skill.setDescription(request.getDescription());

        if (request.getIntentPatterns() != null) {
            skill.setIntentPatterns(serializeIntentPatterns(request.getIntentPatterns()));
        }
        skill.setClassPath(request.getClassPath());

        SkillRegistryEntity updated = skillRegistryRepository.save(skill);
        log.info("技能已更新: {}", skillName);
        return updated;
    }

    @Transactional
    public void deleteSkill(String skillName) {
        SkillRegistryEntity skill = skillRegistryRepository.findByName(skillName)
                .orElseThrow(() -> new ResourceNotFoundException("技能", skillName));
        skillRegistryRepository.delete(skill);
        log.info("技能已删除: {}", skillName);
    }

    @Transactional
    public SkillRegistryEntity updateSkillStatus(String skillName, SkillStatusRequest request) {
        SkillRegistryEntity skill = skillRegistryRepository.findByName(skillName)
                .orElseThrow(() -> new ResourceNotFoundException("技能", skillName));

        skill.setEnabled(request.getEnabled());
        SkillRegistryEntity updated = skillRegistryRepository.save(skill);

        String status = request.getEnabled() ? "启用" : "禁用";
        log.info("技能已{}: {}", status, skillName);
        return updated;
    }

    public Set<String> getSkillNames() {
        return skillMap.keySet();
    }

    private String serializeIntentPatterns(List<String> intentPatterns) {
        if (intentPatterns == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(intentPatterns);
        } catch (JsonProcessingException e) {
            log.error("序列化意图模式失败", e);
            return null;
        }
    }
}
