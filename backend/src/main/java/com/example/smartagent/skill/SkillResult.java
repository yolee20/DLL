
package com.example.smartagent.skill;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillResult {
    private Boolean success;
    private String answer;
    private String skillName;
    private Double confidence;
    private String errorMessage;
    
    public static SkillResult success(String answer, String skillName) {
        return SkillResult.builder()
                .success(true)
                .answer(answer)
                .skillName(skillName)
                .confidence(1.0)
                .build();
    }
    
    public static SkillResult success(String answer, String skillName, Double confidence) {
        return SkillResult.builder()
                .success(true)
                .answer(answer)
                .skillName(skillName)
                .confidence(confidence)
                .build();
    }
    
    public static SkillResult failure(String message) {
        return SkillResult.builder()
                .success(false)
                .answer(message)
                .skillName(null)
                .confidence(0.0)
                .build();
    }
}
