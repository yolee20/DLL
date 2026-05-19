package com.example.smartagent.service.management;

import com.example.smartagent.dto.request.FeedbackRequest;
import com.example.smartagent.entity.AgentConversationEntity;
import com.example.smartagent.entity.QAFeedbackEntity;
import com.example.smartagent.exception.ResourceNotFoundException;
import com.example.smartagent.repository.AgentConversationRepository;
import com.example.smartagent.repository.QAFeedbackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedbackService {

    private final AgentConversationRepository conversationRepository;
    private final QAFeedbackRepository feedbackRepository;

    public void submitFeedback(Long conversationId, FeedbackRequest request) {
        AgentConversationEntity conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("对话记录", conversationId.toString()));

        QAFeedbackEntity feedback = new QAFeedbackEntity();
        feedback.setConversation(conversation);
        feedback.setSatisfaction(request.getSatisfaction());
        feedback.setComment(request.getComment());

        feedbackRepository.save(feedback);
        log.info("Feedback submitted for conversation: {}", conversationId);
    }

    public long countFeedbacks() {
        return feedbackRepository.count();
    }
}
