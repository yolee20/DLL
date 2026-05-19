
package com.example.smartagent.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModelRegisterRequest {
    @NotBlank(message = "模型名称不能为空")
    private String name;
    
    @NotBlank(message = "版本号不能为空")
    private String version;
    
    @NotBlank(message = "模型类型不能为空")
    private String type;
    
    @NotBlank(message = "模型路径不能为空")
    private String path;
}
