
package com.example.smartagent.mcp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelStatus {
    private String name;
    private String version;
    private String type;
    private String status;
    private Long memoryUsage;
    
    public static ModelStatus loaded(String name, String version, String type) {
        return ModelStatus.builder()
                .name(name)
                .version(version)
                .type(type)
                .status("loaded")
                .build();
    }
    
    public static ModelStatus unloaded(String name, String version, String type) {
        return ModelStatus.builder()
                .name(name)
                .version(version)
                .type(type)
                .status("unloaded")
                .build();
    }
}
