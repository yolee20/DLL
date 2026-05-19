package com.example.smartagent.service.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoredMatch {

    private String id;

    private String question;

    private String answer;

    private String category;

    private double score;

    private String source;
}
