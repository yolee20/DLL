
package com.example.smartagent.repository;

import com.example.smartagent.entity.QAFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QAFeedbackRepository extends JpaRepository<QAFeedback, Long> {
    List<QAFeedback> findByConversationId(Long conversationId);
}
