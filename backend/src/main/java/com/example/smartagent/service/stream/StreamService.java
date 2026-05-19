package com.example.smartagent.service.stream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class StreamService {

    private final Map<String, StreamContext> activeStreams = new ConcurrentHashMap<>();

    public StreamContext createStream(String sessionId) {
        StreamContext context = new StreamContext(sessionId);
        activeStreams.put(sessionId, context);
        log.debug("Stream context created for session: {}", sessionId);
        return context;
    }

    public StreamContext getStream(String sessionId) {
        return activeStreams.get(sessionId);
    }

    public void closeStream(String sessionId) {
        StreamContext context = activeStreams.remove(sessionId);
        if (context != null) {
            context.setClosed(true);
            log.debug("Stream closed for session: {}", sessionId);
        }
    }

    public Flux<String> createSSEFlux(String sessionId) {
        StreamContext context = getStream(sessionId);
        if (context == null) {
            context = createStream(sessionId);
        }

        final StreamContext finalContext = context;
        return Flux.create(sink -> {
            finalContext.setSink(sink);
            sink.onRequest(n -> log.debug("SSE sink requested {} elements for session: {}", n, sessionId));
            sink.onCancel(() -> {
                log.debug("SSE sink cancelled for session: {}", sessionId);
                closeStream(sessionId);
            });
        });
    }

    public void pushToken(String sessionId, String token) {
        StreamContext context = getStream(sessionId);
        if (context != null && !context.isClosed() && context.getSink() != null) {
            try {
                String sseEvent = formatSSEEvent("text", token);
                context.getSink().next(sseEvent);
                context.updateActivity();
                log.debug("Pushed token to session: {}", sessionId);
            } catch (Exception e) {
                log.error("Failed to push token to session: {}", sessionId, e);
                closeStream(sessionId);
            }
        }
    }

    public void pushError(String sessionId, String errorMessage) {
        StreamContext context = getStream(sessionId);
        if (context != null && !context.isClosed() && context.getSink() != null) {
            try {
                String sseEvent = formatSSEEvent("error", errorMessage);
                context.getSink().next(sseEvent);
                context.getSink().complete();
                closeStream(sessionId);
            } catch (Exception e) {
                log.error("Failed to push error to session: {}", sessionId, e);
            }
        }
    }

    public void pushDone(String sessionId, String metadata) {
        StreamContext context = getStream(sessionId);
        if (context != null && !context.isClosed() && context.getSink() != null) {
            try {
                String sseEvent = formatSSEEvent("done", metadata);
                context.getSink().next(sseEvent);
                context.getSink().complete();
                closeStream(sessionId);
            } catch (Exception e) {
                log.error("Failed to push done to session: {}", sessionId, e);
            }
        }
    }

    public String formatSSEEvent(String eventType, String data) {
        return String.format("event: %s\ndata: {\"content\":\"%s\"}\n\n",
                eventType, escapeJSON(data));
    }

    public String formatSSEEvent(String eventType, Object data) {
        return String.format("event: %s\ndata: %s\n\n",
                eventType, escapeJSON(data.toString()));
    }

    private String escapeJSON(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    public int getActiveStreamCount() {
        return activeStreams.size();
    }

    public boolean isStreamActive(String sessionId) {
        StreamContext context = getStream(sessionId);
        return context != null && !context.isClosed();
    }

    public static class StreamContext {
        private final String sessionId;
        private final AtomicBoolean closed = new AtomicBoolean(false);
        private FluxSink<String> sink;
        private long lastActivity;

        public StreamContext(String sessionId) {
            this.sessionId = sessionId;
            this.lastActivity = System.currentTimeMillis();
        }

        public String getSessionId() {
            return sessionId;
        }

        public boolean isClosed() {
            return closed.get();
        }

        public void setClosed(boolean closed) {
            this.closed.set(closed);
        }

        public FluxSink<String> getSink() {
            return sink;
        }

        public void setSink(FluxSink<String> sink) {
            this.sink = sink;
        }

        public long getLastActivity() {
            return lastActivity;
        }

        public void updateActivity() {
            this.lastActivity = System.currentTimeMillis();
        }
    }
}