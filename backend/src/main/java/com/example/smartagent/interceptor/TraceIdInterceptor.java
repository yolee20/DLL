package com.example.smartagent.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

@Component
@Slf4j
public class TraceIdInterceptor implements HandlerInterceptor {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String TRACE_ID_MDC_KEY = "traceId";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String traceId = request.getHeader(TRACE_ID_HEADER);

        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString();
        }

        MDC.put(TRACE_ID_MDC_KEY, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);

        log.debug("Request: {} {}, traceId: {}", request.getMethod(), request.getRequestURI(), traceId);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        String traceId = MDC.get(TRACE_ID_MDC_KEY);
        log.debug("Response: {} {}, traceId: {}, status: {}",
                request.getMethod(), request.getRequestURI(), traceId, response.getStatus());

        MDC.remove(TRACE_ID_MDC_KEY);
    }
}
