
package com.example.smartagent.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "qa_pair")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QAPair {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "question", nullable = false, columnDefinition = "TEXT")
    private String question;
    
    @Column(name = "answer", nullable = false, columnDefinition = "TEXT")
    private String answer;
    
    @ManyToOne
    @JoinColumn(name = "category_id")
    private QACategory category;
    
    @Column(name = "vector_json", columnDefinition = "TEXT")
    private String vectorJson;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
