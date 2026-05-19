package com.example.smartagent.repository;

import com.example.smartagent.entity.ModelRegistry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ModelRegistryRepository extends JpaRepository<ModelRegistry, Long> {
    Optional<ModelRegistry> findByModelName(String modelName);
    List<ModelRegistry> findByModelType(String modelType);
    List<ModelRegistry> findByLoaded(Boolean loaded);
}