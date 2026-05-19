package com.example.smartagent.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 技能状态更新请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SkillStatusRequest {

    @NotNull(message = "状态不能为空")
    private Boolean enabled;
}