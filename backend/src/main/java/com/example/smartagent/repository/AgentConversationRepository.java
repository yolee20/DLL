
package com.example.smartagent.repository;

import com.example.smartagent.entity.AgentConversationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgentConversationRepository extends JpaRepository<AgentConversationEntity, Long> {
    List<AgentConversationEntity> findBySessionIdOrderByCreatedAtAsc(String sessionId);
    List<AgentConversationEntity> findByIntent(String intent);
    long countBySkillName(String skillName);
}
