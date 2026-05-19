
package com.example.smartagent.repository;

import com.example.smartagent.entity.QACategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QACategoryRepository extends JpaRepository<QACategory, Long> {
    Optional<QACategory> findByName(String name);
}
