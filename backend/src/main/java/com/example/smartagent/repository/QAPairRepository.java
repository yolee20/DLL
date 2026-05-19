
package com.example.smartagent.repository;

import com.example.smartagent.entity.QAPair;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QAPairRepository extends JpaRepository<QAPair, Long> {
    List<QAPair> findByCategoryId(Long categoryId);
    List<QAPair> findByQuestionContaining(String keyword);
}
