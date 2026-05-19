
package com.example.smartagent.mcp.v2;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelStatus {
    
    private String modelId;
    
    private String modelName;
    
    private String provider;
    
    private String status;
    
    private Long lastUsedTime;
    
    private Long requestCount;
    
    private Double avgResponseTime;
    
    private String description;
    
    private ModelConfig config;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModelConfig {
        private Integer maxTokens;
        private Double temperature;
        private Double topP;
        private Integer contextWindow;
    }
}
