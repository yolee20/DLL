
package com.example.smartagent.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "skill_registry")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillRegistryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name", nullable = false, unique = true, length = 128)
    private String name;
    
    @Column(name = "description", length = 500)
    private String description;
    
    @Column(name = "intent_patterns", columnDefinition = "TEXT")
    private String intentPatterns;
    
    @Column(name = "class_path", nullable = false, length = 256)
    private String classPath;
    
    @Column(name = "enabled")
    @Builder.Default
    private Boolean enabled = true;
    
    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
