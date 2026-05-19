
package com.example.smartagent.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SkillRegisterRequest {
    @NotBlank(message = "技能名称不能为空")
    private String name;
    
    private String description;
    
    private List<String> intentPatterns;
    
    @NotBlank(message = "类路径不能为空")
    private String classPath;
}
