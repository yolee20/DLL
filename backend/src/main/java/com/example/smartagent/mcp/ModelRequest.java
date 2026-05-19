
package com.example.smartagent.mcp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelRequest {
    private String input;
    private Map<String, Object> parameters;
    
    public static ModelRequest of(String input) {
        return ModelRequest.builder()
                .input(input)
                .build();
    }
}
