
package com.example.smartagent.repository;

import com.example.smartagent.entity.QAFeedbackEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QAFeedbackRepository extends JpaRepository<QAFeedbackEntity, Long> {
    List<QAFeedbackEntity> findByConversationId(Long conversationId);
}
