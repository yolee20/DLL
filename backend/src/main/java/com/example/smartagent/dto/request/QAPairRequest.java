
package com.example.smartagent.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QAPairRequest {
    @NotBlank(message = "问题不能为空")
    private String question;
    
    @NotBlank(message = "答案不能为空")
    private String answer;
    
    private Long categoryId;
}
