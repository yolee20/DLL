package com.example.smartagent.agent.handler;

import com.example.smartagent.service.llm.LlmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class ErrorHandler {

    private final LlmService llmService;

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;

    private final Map<String, ErrorContext> errorContexts = new ConcurrentHashMap<>();

    public void init(String sessionId) {
        errorContexts.put(sessionId, new ErrorContext());
        log.debug("ErrorHandler initialized for session: {}", sessionId);
    }

    public ErrorContext getContext(String sessionId) {
        return errorContexts.computeIfAbsent(sessionId, k -> new ErrorContext());
    }

    public <T> T catchError(String sessionId, String operation, ErrorSupplier<T> supplier) {
        ErrorContext context = getContext(sessionId);
        int retryCount = 0;
        Exception lastException = null;

        while (retryCount < MAX_RETRIES) {
            try {
                T result = supplier.get();
                context.recordSuccess(operation);
                return result;
            } catch (Exception e) {
                lastException = e;
                retryCount++;
                context.recordError(operation, e);

                log.warn("Error in operation '{}' for session: {}, attempt {}/{}: {}",
                        operation, sessionId, retryCount, MAX_RETRIES, e.getMessage());

                if (retryCount < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS * retryCount);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        log.error("All retries exhausted for operation '{}' in session: {}", operation, sessionId);
        context.markAsFailed(operation);
        return handleFinalFailure(operation, lastException, context);
    }

    public void catchError(String sessionId, String operation, ErrorRunnable runnable) {
        ErrorContext context = getContext(sessionId);
        int retryCount = 0;

        while (retryCount < MAX_RETRIES) {
            try {
                runnable.run();
                context.recordSuccess(operation);
                return;
            } catch (Exception e) {
                retryCount++;
                context.recordError(operation, e);

                log.warn("Error in operation '{}' for session: {}, attempt {}/{}: {}",
                        operation, sessionId, retryCount, MAX_RETRIES, e.getMessage());

                if (retryCount < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS * retryCount);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        log.error("All retries exhausted for operation '{}' in session: {}", operation, sessionId);
        context.markAsFailed(operation);
    }

    private <T> T handleFinalFailure(String operation, Exception e, ErrorContext context) {
        ErrorType errorType = classifyError(e);

        return switch (errorType) {
            case MODEL_ERROR -> {
                log.warn("Model error detected, switching to fallback");
                yield getFallbackResponse("抱歉，AI模型暂时不可用，请稍后重试。");
            }
            case TOOL_ERROR -> {
                log.warn("Tool error detected, falling back to basic response");
                yield getFallbackResponse("抱歉，工具服务暂时不可用，请换个问题尝试。");
            }
            case RETRIEVAL_ERROR -> {
                log.warn("Retrieval error detected, falling back to direct LLM");
                yield getFallbackResponseFromLLM();
            }
            case TIMEOUT_ERROR -> {
                log.warn("Timeout error detected");
                yield getFallbackResponse("请求超时，请稍后重试。");
            }
            default -> {
                log.error("Unknown error type");
                yield getFallbackResponse("服务暂时不可用，请稍后重试。");
            }
        };
    }

    private ErrorType classifyError(Exception e) {
        String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";

        if (message.contains("timeout") || message.contains("timed out")) {
            return ErrorType.TIMEOUT_ERROR;
        } else if (message.contains("model") || message.contains("openai") ||
                   message.contains("llm") || message.contains("generation")) {
            return ErrorType.MODEL_ERROR;
        } else if (message.contains("tool") || message.contains("function")) {
            return ErrorType.TOOL_ERROR;
        } else if (message.contains("vector") || message.contains("milvus") ||
                   message.contains("embedding") || message.contains("retrieval")) {
            return ErrorType.RETRIEVAL_ERROR;
        }
        return ErrorType.UNKNOWN_ERROR;
    }

    private <T> T getFallbackResponse(String message) {
        if (message instanceof String) {
            return (T) message;
        }
        return (T) message;
    }

    private <T> T getFallbackResponseFromLLM() {
        try {
            String fallback = llmService.generateAnswer("请简单介绍一下你能做什么？");
            return (T) fallback;
        } catch (Exception e) {
            log.error("Fallback LLM call also failed", e);
            return (T) "抱歉，服务暂时不可用，请稍后重试。";
        }
    }

    public void clear(String sessionId) {
        errorContexts.remove(sessionId);
        log.debug("ErrorHandler context cleared for session: {}", sessionId);
    }

    public void markAsDegraded(String sessionId) {
        getContext(sessionId).setDegraded(true);
        log.warn("Session {} marked as degraded", sessionId);
    }

    public boolean isDegraded(String sessionId) {
        ErrorContext context = errorContexts.get(sessionId);
        return context != null && context.isDegraded();
    }

    @FunctionalInterface
    public interface ErrorSupplier<T> {
        T get() throws Exception;
    }

    @FunctionalInterface
    public interface ErrorRunnable {
        void run() throws Exception;
    }

    public enum ErrorType {
        MODEL_ERROR,
        TOOL_ERROR,
        RETRIEVAL_ERROR,
        TIMEOUT_ERROR,
        UNKNOWN_ERROR
    }

    @lombok.Data
    public static class ErrorContext {
        private int totalErrors = 0;
        private int totalRetries = 0;
        private String lastFailedOperation;
        private Exception lastException;
        private boolean degraded = false;
        private Map<String, Integer> errorCounts = new ConcurrentHashMap<>();

        public void recordError(String operation, Exception e) {
            totalErrors++;
            totalRetries++;
            lastFailedOperation = operation;
            lastException = e;
            errorCounts.merge(operation, 1, Integer::sum);
        }

        public void recordSuccess(String operation) {
            lastFailedOperation = null;
            lastException = null;
        }

        public void markAsFailed(String operation) {
            lastFailedOperation = operation;
        }
    }
}
