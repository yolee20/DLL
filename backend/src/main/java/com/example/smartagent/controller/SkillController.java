package com.example.smartagent.controller;

import com.example.smartagent.dto.request.SkillRegisterRequest;
import com.example.smartagent.dto.request.SkillStatusRequest;
import com.example.smartagent.dto.response.ApiResponse;
import com.example.smartagent.entity.SkillRegistry;
import com.example.smartagent.exception.ResourceNotFoundException;
import com.example.smartagent.repository.SkillRegistryRepository;
import com.example.smartagent.skill.Skill;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 技能控制器
 * 提供技能管理相关的 REST API 接口
 */
@RestController
@RequestMapping("/api/skills")
@RequiredArgsConstructor
@Slf4j
public class SkillController {

    private final SkillRegistryRepository skillRegistryRepository;
    private final Map<String, Skill> skillMap;
    private final ObjectMapper objectMapper;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SkillRegistry>>> getAllSkills() {
        List<SkillRegistry> skills = skillRegistryRepository.findAll();
        return ResponseEntity.ok(ApiResponse.success(skills));
    }

    @GetMapping("/{skillName}")
    public ResponseEntity<ApiResponse<SkillRegistry>> getSkill(@PathVariable String skillName) {
        SkillRegistry skill = skillRegistryRepository.findByName(skillName)
                .orElseThrow(() -> new ResourceNotFoundException("技能", skillName));
        return ResponseEntity.ok(ApiResponse.success(skill));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SkillRegistry>> createSkill(@Valid @RequestBody SkillRegisterRequest request) {
        String intentPatternsJson = serializeIntentPatterns(request.getIntentPatterns());
        
        SkillRegistry skill = SkillRegistry.builder()
                .name(request.getName())
                .description(request.getDescription())
                .intentPatterns(intentPatternsJson)
                .classPath(request.getClassPath())
                .enabled(true)
                .build();
        
        SkillRegistry saved = skillRegistryRepository.save(skill);
        log.info("技能已创建: {}", saved.getName());
        return ResponseEntity.ok(ApiResponse.success("技能创建成功", saved));
    }

    @PutMapping("/{skillName}")
    public ResponseEntity<ApiResponse<SkillRegistry>> updateSkill(
            @PathVariable String skillName,
            @Valid @RequestBody SkillRegisterRequest request) {
        
        SkillRegistry skill = skillRegistryRepository.findByName(skillName)
                .orElseThrow(() -> new ResourceNotFoundException("技能", skillName));
        
        skill.setDescription(request.getDescription());
        
        if (request.getIntentPatterns() != null) {
            skill.setIntentPatterns(serializeIntentPatterns(request.getIntentPatterns()));
        }
        skill.setClassPath(request.getClassPath());
        
        SkillRegistry updated = skillRegistryRepository.save(skill);
        log.info("技能已更新: {}", skillName);
        return ResponseEntity.ok(ApiResponse.success("技能更新成功", updated));
    }

    @DeleteMapping("/{skillName}")
    public ResponseEntity<ApiResponse<Void>> deleteSkill(@PathVariable String skillName) {
        SkillRegistry skill = skillRegistryRepository.findByName(skillName)
                .orElseThrow(() -> new ResourceNotFoundException("技能", skillName));
        
        skillRegistryRepository.delete(skill);
        log.info("技能已删除: {}", skillName);
        return ResponseEntity.ok(ApiResponse.success("技能删除成功", null));
    }

    /**
     * 更新技能状态（启用/禁用）
     */
    @PutMapping("/{skillName}/status")
    public ResponseEntity<ApiResponse<SkillRegistry>> updateSkillStatus(
            @PathVariable String skillName,
            @Valid @RequestBody SkillStatusRequest request) {
        
        SkillRegistry skill = skillRegistryRepository.findByName(skillName)
                .orElseThrow(() -> new ResourceNotFoundException("技能", skillName));
        
        skill.setEnabled(request.getEnabled());
        SkillRegistry updated = skillRegistryRepository.save(skill);
        
        String status = request.getEnabled() ? "启用" : "禁用";
        log.info("技能已{}: {}", status, skillName);
        return ResponseEntity.ok(ApiResponse.success("技能已" + status, updated));
    }

    @GetMapping("/names")
    public ResponseEntity<ApiResponse<Set<String>>> getSkillNames() {
        return ResponseEntity.ok(ApiResponse.success(skillMap.keySet()));
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