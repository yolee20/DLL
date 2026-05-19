package com.example.smartagent.controller;

import com.example.smartagent.agent.core.SmartAgent;
import com.example.smartagent.agent.core.SpringAISmartAgent;
import com.example.smartagent.dto.request.ChatRequest;
import com.example.smartagent.service.ratelimit.RateLimitResult;
import com.example.smartagent.service.ratelimit.RateLimitService;
import com.example.smartagent.service.stream.StreamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.UUID;

@RestController
@RequestMapping("/api/stream")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "流式对话", description = "SSE流式对话接口 - 采用Spring AI实现")
public class StreamController {

    private final SmartAgent smartAgent;
    private final SpringAISmartAgent springAISmartAgent;
    private final StreamService streamService;
    private final RateLimitService rateLimitService;

    @Operation(summary = "发送流式聊天消息（Spring AI版本）", description = "向Spring AI Agent发送消息并通过SSE流式获取响应")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "成功建立SSE连接"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "请求过于频繁，被限流"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数无效")
    })
    @PostMapping(value = "/chat/springai", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChatSpringAI(@Valid @RequestBody ChatRequest request) {
        String traceId = UUID.randomUUID().toString();
        String sessionId = request.getSessionId() != null && !request.getSessionId().isEmpty()
                ? request.getSessionId()
                : UUID.randomUUID().toString();

        log.info("[{}] Spring AI Stream chat: {}", traceId, request.getMessage());

        RateLimitResult limitResult = rateLimitService.allowRequest(request.getUserId());
        if (!limitResult.isAllowed()) {
            return Flux.just("event: error\ndata: {\"error\":\"Rate limit exceeded\"}\n\n");
        }

        request.setSessionId(sessionId);
        streamService.createStream(sessionId);

        Flux<String> tokenFlux = springAISmartAgent.chatStream(request);

        return tokenFlux
                .doOnNext(token -> streamService.pushToken(sessionId, token))
                .map(token -> "event: chunk\ndata: {\"content\":\"" + escapeJSON(token) + "\"}\n\n")
                .concatWithValues("event: complete\ndata: {\"sessionId\":\"" + sessionId + "\"}\n\n");
    }

    @Operation(summary = "发送流式聊天消息（原版本）", description = "向Agent发送消息并通过SSE流式获取响应")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "成功建立SSE连接"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "请求过于频繁，被限流"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数无效")
    })
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(@Valid @RequestBody ChatRequest request) {
        String traceId = UUID.randomUUID().toString();
        String sessionId = request.getSessionId() != null
                ? request.getSessionId()
                : UUID.randomUUID().toString();

        log.info("[{}] Stream chat request for session: {}, message: {}",
                traceId, sessionId, request.getMessage());

        RateLimitResult limitResult = rateLimitService.allowRequest(request.getUserId());
        if (!limitResult.isAllowed()) {
            log.warn("[{}] Rate limit exceeded for user: {}", traceId, request.getUserId());
            return Flux.just("event: error\ndata: {\"error\":\"Rate limit exceeded\",\"retryAfter\":" +
                    limitResult.getRetryAfterSeconds() + "}\n\n");
        }

        request.setSessionId(sessionId);
        streamService.createStream(sessionId);

        Flux<SmartAgent.StreamEvent> eventFlux = smartAgent.chatStream(request);

        return eventFlux.map(event -> {
            String sseEvent;
            switch (event.type()) {
                case "intent" -> sseEvent = String.format(
                        "event: intent\ndata: {\"intent\":\"%s\",\"confidence\":%s,\"skillUsed\":\"%s\"}\n\n",
                        event.intent(), event.confidence(), event.skillUsed());
                case "chunk" -> {
                    streamService.pushToken(sessionId, event.content());
                    sseEvent = String.format("event: chunk\ndata: {\"content\":\"%s\"}\n\n",
                            escapeJSON(event.content()));
                }
                case "complete" -> {
                    streamService.pushDone(sessionId, "{\"sessionId\":\"" + event.sessionId() + "\"}");
                    sseEvent = String.format("event: complete\ndata: {\"sessionId\":\"%s\"}\n\n",
                            event.sessionId());
                }
                case "error" -> {
                    streamService.pushError(sessionId, event.content());
                    sseEvent = String.format("event: error\ndata: {\"error\":\"%s\"}\n\n",
                            escapeJSON(event.content()));
                }
                default -> sseEvent = "";
            }
            return sseEvent;
        });
    }

    @Operation(summary = "获取流状态", description = "查询指定会话的SSE流是否处于活跃状态")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功")
    })
    @GetMapping("/status/{sessionId}")
    public ResponseEntity<StreamStatus> getStreamStatus(
            @Parameter(description = "会话ID") @PathVariable String sessionId) {
        boolean isActive = streamService.isStreamActive(sessionId);
        return ResponseEntity.ok(new StreamStatus(sessionId, isActive));
    }

    @Operation(summary = "关闭流连接", description = "手动关闭指定会话的SSE流连接")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "关闭成功")
    })
    @DeleteMapping("/close/{sessionId}")
    public ResponseEntity<Void> closeStream(
            @Parameter(description = "会话ID") @PathVariable String sessionId) {
        streamService.closeStream(sessionId);
        log.info("Stream manually closed for session: {}", sessionId);
        return ResponseEntity.ok().build();
    }

    private String escapeJSON(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public record StreamStatus(String sessionId, boolean active) {}
}
