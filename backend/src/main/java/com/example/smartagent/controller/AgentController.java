package com.example.smartagent.controller;

import com.example.smartagent.agent.SmartAgent;
import com.example.smartagent.dto.request.ChatRequest;
import com.example.smartagent.dto.request.FeedbackRequest;
import com.example.smartagent.dto.response.ApiResponse;
import com.example.smartagent.dto.response.ChatResponse;
import com.example.smartagent.dto.response.ConversationResponse;
import com.example.smartagent.dto.response.SessionResponse;
import com.example.smartagent.entity.AgentConversation;
import com.example.smartagent.entity.QAFeedback;
import com.example.smartagent.exception.ResourceNotFoundException;
import com.example.smartagent.repository.AgentConversationRepository;
import com.example.smartagent.repository.QAFeedbackRepository;
import com.example.smartagent.service.LlmService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Agent 控制器
 * 提供智能代理相关的 REST API 接口
 */
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
@Slf4j
public class AgentController {

    private final SmartAgent smartAgent;
    private final AgentConversationRepository conversationRepository;
    private final QAFeedbackRepository feedbackRepository;
    private final LlmService llmService;

    /**
     * 对话接口 - 与智能代理进行对话
     */
    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<ChatResponse>> chat(@Valid @RequestBody ChatRequest request) {
        log.info("Received chat request: {}", request.getMessage());
        ChatResponse response = smartAgent.chat(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 获取所有会话列表
     */
    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse<List<String>>> getAllSessions() {
        List<String> sessions = conversationRepository.findAll().stream()
                .map(AgentConversation::getSessionId)
                .distinct()
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(sessions));
    }

    /**
     * 获取会话详情
     */
    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<ApiResponse<SessionResponse>> getSession(@PathVariable String sessionId) {
        List<AgentConversation> conversations = conversationRepository
                .findBySessionIdOrderByCreatedAtAsc(sessionId);

        List<ConversationResponse> messages = conversations.stream()
                .map(c -> ConversationResponse.builder()
                        .id(c.getId())
                        .sessionId(c.getSessionId())
                        .userMessage(c.getUserMessage())
                        .intent(c.getIntent())
                        .intentConfidence(c.getIntentConfidence() != null ? c.getIntentConfidence().doubleValue() : null)
                        .skillName(c.getSkillName())
                        .agentResponse(c.getAgentResponse())
                        .createdAt(c.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        SessionResponse response = SessionResponse.builder()
                .sessionId(sessionId)
                .messages(messages)
                .createdAt(messages.isEmpty() ? null : messages.get(0).getCreatedAt())
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 删除会话（从内存中移除上下文）
     */
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<ApiResponse<Void>> deleteSession(@PathVariable String sessionId) {
        smartAgent.endSession(sessionId);
        return ResponseEntity.ok(ApiResponse.success("会话已结束", null));
    }

    /**
     * 提交对话反馈
     */
    @PostMapping("/conversations/{conversationId}/feedback")
    public ResponseEntity<ApiResponse<Void>> submitFeedback(
            @PathVariable Long conversationId,
            @Valid @RequestBody FeedbackRequest request) {
        
        AgentConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("对话记录", conversationId.toString()));

        QAFeedback feedback = new QAFeedback();
        feedback.setConversation(conversation);
        feedback.setSatisfaction(request.getSatisfaction());
        feedback.setComment(request.getComment());

        feedbackRepository.save(feedback);
        return ResponseEntity.ok(ApiResponse.success("反馈提交成功", null));
    }

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("SmartAgent is running", "OK"));
    }

    /**
     * 流式聊天接口
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@Valid @RequestBody ChatRequest request) {
        return Flux.create(sink -> {
            try {
                ChatResponse response = smartAgent.chat(request);

                if (response != null && response.getResponse() != null) {
                    String answer = response.getResponse();
                    int chunkSize = 10;

                    for (int i = 0; i < answer.length(); i += chunkSize) {
                        int end = Math.min(i + chunkSize, answer.length());
                        sink.next(answer.substring(i, end));
                    }
                } else {
                    String answer = llmService.generateAnswer(request.getMessage());
                    int chunkSize = 10;

                    for (int i = 0; i < answer.length(); i += chunkSize) {
                        int end = Math.min(i + chunkSize, answer.length());
                        sink.next(answer.substring(i, end));
                    }
                }

                sink.complete();
            } catch (Exception e) {
                sink.error(e);
            }
        });
    }
}