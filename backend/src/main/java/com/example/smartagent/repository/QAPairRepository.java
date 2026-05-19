
package com.example.smartagent.repository;

import com.example.smartagent.entity.QAPairEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QAPairRepository extends JpaRepository<QAPairEntity, Long> {
    List<QAPairEntity> findByCategoryId(Long categoryId);
    List<QAPairEntity> findByQuestionContaining(String keyword);
}
