package com.example.smartagent.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "model_registry")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelRegistry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "model_name", nullable = false, unique = true, length = 100)
    private String modelName;
    
    @Column(name = "model_type", nullable = false, length = 50)
    private String modelType;
    
    @Column(name = "provider", length = 50)
    private String provider;
    
    @Column(name = "api_key", length = 255)
    private String apiKey;
    
    @Column(name = "endpoint_url", length = 500)
    private String endpointUrl;
    
    @Column(name = "parameters", columnDefinition = "TEXT")
    private String parameters;
    
    @Column(name = "loaded")
    @Builder.Default
    private Boolean loaded = false;
    
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