
package com.example.smartagent.repository;

import com.example.smartagent.entity.SkillRegistryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SkillRegistryRepository extends JpaRepository<SkillRegistryEntity, Long> {
    Optional<SkillRegistryEntity> findByName(String name);
    List<SkillRegistryEntity> findByEnabled(Boolean enabled);
}
