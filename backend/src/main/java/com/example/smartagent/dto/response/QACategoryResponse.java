
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
public class QACategoryResponse {
    private Long id;
    private String name;
    private String description;
    private LocalDateTime createdAt;
}
