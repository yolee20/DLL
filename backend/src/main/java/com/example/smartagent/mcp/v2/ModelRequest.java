
package com.example.smartagent.mcp.v2;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelRequest {
    
    private String modelId;
    
    private List<Message> messages;
    
    private Integer maxTokens;
    
    private Double temperature;
    
    private String prompt;
    
    private Map<String, Object> parameters;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        private String role;
        private String content;
    }
}
