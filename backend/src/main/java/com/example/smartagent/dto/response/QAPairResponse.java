
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
public class QAPairResponse {
    private Long id;
    private String question;
    private String answer;
    private Long categoryId;
    private String categoryName;
    private LocalDateTime createdAt;
}
