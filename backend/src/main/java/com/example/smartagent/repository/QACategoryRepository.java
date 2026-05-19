
package com.example.smartagent.repository;

import com.example.smartagent.entity.QACategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QACategoryRepository extends JpaRepository<QACategoryEntity, Long> {
    Optional<QACategoryEntity> findByName(String name);
}
