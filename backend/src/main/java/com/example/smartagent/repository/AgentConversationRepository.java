
package com.example.smartagent.repository;

import com.example.smartagent.entity.AgentConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgentConversationRepository extends JpaRepository<AgentConversation, Long> {
    List<AgentConversation> findBySessionIdOrderByCreatedAtAsc(String sessionId);
    List<AgentConversation> findByIntent(String intent);
    long countBySkillName(String skillName);
}
