
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
public class ModelResponse {
    private Long id;
    private String name;
    private String version;
    private String type;
    private String path;
    private String status;
    private LocalDateTime createdAt;
}
