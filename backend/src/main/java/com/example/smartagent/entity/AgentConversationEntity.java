
package com.example.smartagent.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "agent_conversation", indexes = {
    @Index(name = "idx_session_id", columnList = "session_id"),
    @Index(name = "idx_tenant_id", columnList = "tenant_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentConversationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", length = 64)
    private String tenantId;

    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;

    @Column(name = "user_id", length = 64)
    private String userId;

    @Column(name = "user_message", nullable = false, columnDefinition = "TEXT")
    private String userMessage;

    @Column(name = "intent", length = 64)
    private String intent;

    @Column(name = "intent_confidence", precision = 5, scale = 4)
    private BigDecimal intentConfidence;

    @Column(name = "skill_name", length = 128)
    private String skillName;

    @Column(name = "agent_response", columnDefinition = "TEXT")
    private String agentResponse;

    @Column(name = "response_time_ms")
    private Integer responseTimeMs;

    @Column(name = "tokens_used")
    private Integer tokensUsed;

    @Column(name = "context_json", columnDefinition = "TEXT")
    private String contextJson;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
