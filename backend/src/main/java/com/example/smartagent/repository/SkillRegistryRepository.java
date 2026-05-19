
package com.example.smartagent.repository;

import com.example.smartagent.entity.SkillRegistry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SkillRegistryRepository extends JpaRepository<SkillRegistry, Long> {
    Optional<SkillRegistry> findByName(String name);
    List<SkillRegistry> findByEnabled(Boolean enabled);
}
