package com.example.smartagent.controller;

import com.example.smartagent.agent.core.SmartAgent;
import com.example.smartagent.agent.core.SpringAISmartAgent;
import com.example.smartagent.dto.request.ChatRequest;
import com.example.smartagent.dto.request.FeedbackRequest;
import com.example.smartagent.dto.response.ApiResponse;
import com.example.smartagent.dto.response.ChatResponse;
import com.example.smartagent.dto.response.SessionResponse;
import com.example.smartagent.memory.SessionManager;
import com.example.smartagent.service.ratelimit.RateLimitService;
import com.example.smartagent.service.ratelimit.RateLimitResult;
import com.example.smartagent.service.conversation.ConversationService;
import com.example.smartagent.service.management.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.document.Document;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Agent对话", description = "智能Agent对话、会话管理、反馈接口 - 采用Spring AI实现")
public class AgentController {

    private final SmartAgent smartAgent;
    private final SpringAISmartAgent springAISmartAgent;
    private final ConversationService conversationService;
    private final FeedbackService feedbackService;
    private final SessionManager sessionManager;
    private final RateLimitService rateLimitService;

    @Operation(summary = "发送聊天消息（Spring AI版本）", description = "向Spring AI Agent发送消息并获取响应（非流式）")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "处理成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "请求过于频繁，被限流"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数无效")
    })
    @PostMapping("/chat/springai")
    public ResponseEntity<ApiResponse<ChatResponse>> chatSpringAI(@Valid @RequestBody ChatRequest request) {
        String traceId = UUID.randomUUID().toString();
        log.info("[{}] Spring AI Chat request: {}", traceId, request.getMessage());

        RateLimitResult limitResult = rateLimitService.allowRequest(request.getUserId());
        if (!limitResult.isAllowed()) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", String.valueOf(limitResult.getRetryAfterSeconds()))
                    .body(ApiResponse.error("Rate limit exceeded. Please try again later."));
        }

        ChatResponse response = springAISmartAgent.chat(request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "发送聊天消息（原版本）", description = "向原Agent发送消息并获取响应（非流式）")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "处理成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "请求过于频繁，被限流"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数无效")
    })
    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<ChatResponse>> chat(@Valid @RequestBody ChatRequest request) {
        String traceId = UUID.randomUUID().toString();
        log.info("[{}] Received chat request: {}", traceId, request.getMessage());

        RateLimitResult limitResult = rateLimitService.allowRequest(request.getUserId());
        if (!limitResult.isAllowed()) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", String.valueOf(limitResult.getRetryAfterSeconds()))
                    .body(ApiResponse.error("Rate limit exceeded. Please try again later."));
        }

        sessionManager.addUserMessage(request.getSessionId(), request.getMessage());

        ChatResponse response = smartAgent.chat(request);

        sessionManager.addAssistantMessage(request.getSessionId(), response.getResponse());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "RAG问答（Spring AI版本）", description = "使用Spring AI RAG引擎进行问答")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "处理成功")
    })
    @PostMapping("/rag/springai")
    public ResponseEntity<ApiResponse<ChatResponse>> ragChatSpringAI(@Valid @RequestBody ChatRequest request) {
        log.info("Spring AI RAG Chat request: {}", request.getMessage());
        ChatResponse response = springAISmartAgent.ragChat(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "添加文档到Spring AI知识库", description = "批量添加文档到向量知识库")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "添加成功")
    })
    @PostMapping("/knowledge/documents")
    public ResponseEntity<ApiResponse<Void>> addKnowledgeDocuments(@RequestBody List<Map<String, String>> documents) {
        List<Document> docs = new ArrayList<>();
        for (Map<String, String> doc : documents) {
            String id = doc.getOrDefault("id", UUID.randomUUID().toString());
            String content = doc.get("content");
            if (content != null) {
                // 使用Spring AI Document的正确构造方式
                docs.add(new Document(content, Map.of("id", id)));
            }
        }
        springAISmartAgent.addKnowledgeDocuments(docs);
        return ResponseEntity.ok(ApiResponse.success("文档添加成功", null));
    }

    @Operation(summary = "删除Spring AI知识库文档", description = "删除指定ID的文档")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "删除成功")
    })
    @DeleteMapping("/knowledge/documents")
    public ResponseEntity<ApiResponse<Void>> deleteKnowledgeDocuments(@RequestBody List<String> docIds) {
        springAISmartAgent.deleteKnowledgeDocuments(docIds);
        return ResponseEntity.ok(ApiResponse.success("文档删除成功", null));
    }

    @Operation(summary = "获取所有会话ID列表", description = "查询系统中所有活跃的会话ID")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功")
    })
    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse<List<String>>> getAllSessions() {
        List<String> sessions = conversationService.getAllSessions();
        return ResponseEntity.ok(ApiResponse.success(sessions));
    }

    @Operation(summary = "获取指定会话详情", description = "根据会话ID查询该会话的所有消息历史")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功")
    })
    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<ApiResponse<SessionResponse>> getSession(
            @Parameter(description = "会话ID") @PathVariable String sessionId) {
        SessionResponse response = conversationService.getSession(sessionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "删除会话", description = "删除指定会话及其所有历史消息")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "删除成功")
    })
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<ApiResponse<Void>> deleteSession(
            @Parameter(description = "会话ID") @PathVariable String sessionId) {
        smartAgent.endSession(sessionId);
        conversationService.deleteSession(sessionId);
        return ResponseEntity.ok(ApiResponse.success("会话已结束", null));
    }

    @Operation(summary = "提交对话反馈", description = "对指定的对话记录提交用户反馈")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "提交成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "对话记录不存在")
    })
    @PostMapping("/conversations/{conversationId}/feedback")
    public ResponseEntity<ApiResponse<Void>> submitFeedback(
            @Parameter(description = "对话记录ID") @PathVariable Long conversationId,
            @Valid @RequestBody FeedbackRequest request) {
        feedbackService.submitFeedback(conversationId, request);
        return ResponseEntity.ok(ApiResponse.success("反馈提交成功", null));
    }

    @Operation(summary = "健康检查", description = "检查Agent服务是否正常运行")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "服务正常")
    })
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("SmartAgent (with Spring AI) is running", "OK"));
    }

    @Operation(summary = "获取活跃会话数量", description = "查询当前系统中的活跃会话数量")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功")
    })
    @GetMapping("/metrics/sessions")
    public ResponseEntity<ApiResponse<Long>> getActiveSessionCount() {
        long count = sessionManager.getActiveSessionCount();
        return ResponseEntity.ok(ApiResponse.success(count));
    }
}
