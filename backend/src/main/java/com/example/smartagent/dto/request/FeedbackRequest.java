
package com.example.smartagent.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackRequest {
    @NotNull(message = "会话ID不能为空")
    private Long conversationId;
    
    @NotNull(message = "满意度不能为空")
    @Min(value = 0, message = "满意度最小值为0")
    @Max(value = 10, message = "满意度最大值为10")
    private Integer satisfaction;
    
    private String comment;
}
