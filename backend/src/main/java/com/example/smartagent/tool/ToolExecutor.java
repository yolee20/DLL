package com.example.smartagent.tool;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.*;

@Component
@Slf4j
public class ToolExecutor {

    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final Map<String, Tool> registeredTools = new ConcurrentHashMap<>();

    public void registerTool(Tool tool) {
        registeredTools.put(tool.getName(), tool);
        log.info("Registered tool: {}", tool.getName());
    }

    public ToolResult execute(String toolName, Map<String, Object> params) {
        return execute(toolName, params, 30, TimeUnit.SECONDS);
    }

    public ToolResult execute(String toolName, Map<String, Object> params, long timeout, TimeUnit timeUnit) {
        Tool tool = registeredTools.get(toolName);
        if (tool == null) {
            log.error("Tool not found: {}", toolName);
            return ToolResult.failure("Tool not found: " + toolName);
        }

        try {
            Future<ToolResult> future = executorService.submit(() -> {
                try {
                    return tool.execute(params);
                } catch (Exception e) {
                    log.error("Tool execution failed: {}", toolName, e);
                    return ToolResult.failure("Tool execution failed: " + e.getMessage());
                }
            });

            ToolResult result = future.get(timeout, timeUnit);
            log.info("Tool executed successfully: {}, result: {}", toolName, result.isSuccess());
            return result;

        } catch (TimeoutException e) {
            log.error("Tool execution timeout: {}", toolName);
            return ToolResult.failure("Tool execution timeout: " + timeout + " " + timeUnit);
        } catch (Exception e) {
            log.error("Tool execution failed: {}", toolName, e);
            return ToolResult.failure("Tool execution failed: " + e.getMessage());
        }
    }

    public ToolResult executeWithRetry(String toolName, Map<String, Object> params, int maxRetries, long retryDelayMs) {
        int retryCount = 0;
        Exception lastException = null;

        while (retryCount < maxRetries) {
            try {
                ToolResult result = execute(toolName, params);
                if (result.isSuccess()) {
                    return result;
                }
                log.warn("Tool execution failed, retrying: {}/{}", retryCount + 1, maxRetries);
            } catch (Exception e) {
                lastException = e;
                log.warn("Tool execution exception, retrying: {}/{}", retryCount + 1, maxRetries, e);
            }

            retryCount++;
            if (retryCount < maxRetries) {
                try {
                    Thread.sleep(retryDelayMs * retryCount);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        log.error("All retries exhausted for tool: {}", toolName);
        return ToolResult.failure("All retries exhausted for tool: " + toolName);
    }

    public boolean validateParams(String toolName, Map<String, Object> params) {
        Tool tool = registeredTools.get(toolName);
        if (tool == null) {
            return false;
        }
        return tool.validateParams(params);
    }

    public interface Tool {
        String getName();
        String getDescription();
        ToolResult execute(Map<String, Object> params);
        boolean validateParams(Map<String, Object> params);
    }

    @Data
    @Builder
    public static class ToolResult {
        private final boolean success;
        private final Object data;
        private final String error;
        private final long executionTimeMs;

        public static ToolResult success(Object data) {
            return ToolResult.builder()
                    .success(true)
                    .data(data)
                    .executionTimeMs(System.currentTimeMillis())
                    .build();
        }

        public static ToolResult failure(String error) {
            return ToolResult.builder()
                    .success(false)
                    .error(error)
                    .executionTimeMs(System.currentTimeMillis())
                    .build();
        }
    }

    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
