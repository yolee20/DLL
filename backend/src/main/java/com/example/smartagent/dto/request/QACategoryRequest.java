package com.example.smartagent.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 问答分类创建/更新请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QACategoryRequest {

    @NotBlank(message = "分类名称不能为空")
    private String name;

    private String description;
}