
package com.example.smartagent.repository;

import com.example.smartagent.entity.ModelRegistryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ModelRegistryRepository extends JpaRepository<ModelRegistryEntity, Long> {
    Optional<ModelRegistryEntity> findByModelName(String modelName);
    List<ModelRegistryEntity> findByModelType(String modelType);
    List<ModelRegistryEntity> findByLoaded(Boolean loaded);
}
