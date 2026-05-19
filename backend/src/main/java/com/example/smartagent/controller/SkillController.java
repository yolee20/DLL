package com.example.smartagent.controller;

import com.example.smartagent.dto.request.SkillRegisterRequest;
import com.example.smartagent.dto.request.SkillStatusRequest;
import com.example.smartagent.dto.response.ApiResponse;
import com.example.smartagent.entity.SkillRegistryEntity;
import com.example.smartagent.service.management.SkillManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/skills")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "技能管理", description = "技能注册、启用、禁用等管理接口")
public class SkillController {

    private final SkillManagementService skillManagementService;

    @Operation(summary = "获取所有技能", description = "查询系统中所有已注册的技能列表")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<SkillRegistryEntity>>> getAllSkills() {
        List<SkillRegistryEntity> skills = skillManagementService.getAllSkills();
        return ResponseEntity.ok(ApiResponse.success(skills));
    }

    @Operation(summary = "获取指定技能", description = "根据技能名称查询技能详情")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "技能不存在")
    })
    @GetMapping("/{skillName}")
    public ResponseEntity<ApiResponse<SkillRegistryEntity>> getSkill(
            @Parameter(description = "技能名称") @PathVariable String skillName) {
        SkillRegistryEntity skill = skillManagementService.getSkill(skillName);
        return ResponseEntity.ok(ApiResponse.success(skill));
    }

    @Operation(summary = "注册新技能", description = "向系统注册一个新的技能")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "创建成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数无效")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<SkillRegistryEntity>> createSkill(
            @Valid @RequestBody SkillRegisterRequest request) {
        SkillRegistryEntity skill = skillManagementService.createSkill(request);
        return ResponseEntity.ok(ApiResponse.success("技能创建成功", skill));
    }

    @Operation(summary = "更新技能", description = "更新已注册技能的基本信息")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "更新成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "技能不存在")
    })
    @PutMapping("/{skillName}")
    public ResponseEntity<ApiResponse<SkillRegistryEntity>> updateSkill(
            @Parameter(description = "技能名称") @PathVariable String skillName,
            @Valid @RequestBody SkillRegisterRequest request) {
        SkillRegistryEntity skill = skillManagementService.updateSkill(skillName, request);
        return ResponseEntity.ok(ApiResponse.success("技能更新成功", skill));
    }

    @Operation(summary = "删除技能", description = "从系统中删除指定的技能")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "删除成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "技能不存在")
    })
    @DeleteMapping("/{skillName}")
    public ResponseEntity<ApiResponse<Void>> deleteSkill(
            @Parameter(description = "技能名称") @PathVariable String skillName) {
        skillManagementService.deleteSkill(skillName);
        return ResponseEntity.ok(ApiResponse.success("技能删除成功", null));
    }

    @Operation(summary = "更新技能状态", description = "启用或禁用指定的技能")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "更新成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "技能不存在")
    })
    @PutMapping("/{skillName}/status")
    public ResponseEntity<ApiResponse<SkillRegistryEntity>> updateSkillStatus(
            @Parameter(description = "技能名称") @PathVariable String skillName,
            @Valid @RequestBody SkillStatusRequest request) {
        SkillRegistryEntity skill = skillManagementService.updateSkillStatus(skillName, request);
        return ResponseEntity.ok(ApiResponse.success(skill));
    }

    @Operation(summary = "获取所有技能名称", description = "查询系统中所有已加载的技能名称")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功")
    })
    @GetMapping("/names")
    public ResponseEntity<ApiResponse<Set<String>>> getSkillNames() {
        Set<String> names = skillManagementService.getSkillNames();
        return ResponseEntity.ok(ApiResponse.success(names));
    }
}
