package com.example.smartagent.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    @NotBlank(message = "消息内容不能为空")
    private String message;

    private String sessionId;

    private String userId;

    private String tenantId;

    private Map<String, Object> context;
}
