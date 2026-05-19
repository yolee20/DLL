
package com.example.smartagent.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationResponse {
    private Long id;
    private String sessionId;
    private String userMessage;
    private String intent;
    private Double intentConfidence;
    private String skillName;
    private String agentResponse;
    private LocalDateTime createdAt;
}
