
package com.example.smartagent.mcp.v2;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelRegistration {
    
    private String modelId;
    
    private String modelName;
    
    private String provider;
    
    private String baseUrl;
    
    private String apiKey;
    
    private ModelStatus.ModelConfig defaultConfig;
    
    private Map<String, String> metadata;
}
