package com.example.aitourism.monitor;

import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.output.TokenUsage;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

// AI 调用监听器
// 主要是记录与模型相关的指标
@Component
@Slf4j
public class AiModelMonitorListener implements ChatModelListener {

    // 用于存储请求开始时间的键
    private static final String REQUEST_START_TIME_KEY = "request_start_time";
    // 用于监控上下文传递（因为请求和响应事件的触发不是同一个线程）
    private static final String MONITOR_CONTEXT_KEY = "monitor_context";
    
    @Resource
    private AiModelMetricsCollector aiModelMetricsCollector;

    @Override
    public void onRequest(ChatModelRequestContext requestContext) {
        // 记录请求开始时间
        requestContext.attributes().put(REQUEST_START_TIME_KEY, Instant.now());
        // 从监控上下文中获取信息
        MonitorContext context = MonitorContextHolder.getContext();
        if (context == null) {
            log.warn("MonitorContext is null in onRequest; skip metrics for this request");
            return;
        }
        String userId = context.getUserId();
        String sessionId = context.getSessionId();
        requestContext.attributes().put(MONITOR_CONTEXT_KEY, context);
        // 获取模型名称
        String modelName = requestContext.chatRequest().modelName();
        // 记录请求指标
        aiModelMetricsCollector.recordRequest(userId, sessionId, modelName, "started");
    }

    @Override
    public void onResponse(ChatModelResponseContext responseContext) {
        // 从属性中获取监控信息（由 onRequest 方法存储）
        Map<Object, Object> attributes = responseContext.attributes();
        // 从监控上下文中获取信息
        MonitorContext context = (MonitorContext) attributes.get(MONITOR_CONTEXT_KEY);
        if (context == null) {
            context = MonitorContextHolder.getContext();
        }
        if (context == null) {
            context = MonitorContextHolder.pollFallbackContext();
        }
        if (context == null) {
            log.warn("MonitorContext is null in onResponse; skip metrics for this response");
            return;
        }
        String userId = context.getUserId();
        String sessionId = context.getSessionId();
        // 获取模型名称
        String modelName = responseContext.chatResponse().modelName();
        // 记录成功请求
        aiModelMetricsCollector.recordRequest(userId, sessionId, modelName, "success");
        // 记录响应时间
        recordResponseTime(attributes, userId, sessionId, modelName);
        // 记录 Token 使用情况
        recordTokenUsage(responseContext, userId, sessionId, modelName);
    }

    @Override
    public void onError(ChatModelErrorContext errorContext) {
        // 优先从属性或线程上下文中获取信息
        Map<Object, Object> attributes = errorContext.attributes();
        MonitorContext context = attributes != null ? (MonitorContext) attributes.get(MONITOR_CONTEXT_KEY) : null;
        if (context == null) {
            context = MonitorContextHolder.getContext();
        }
        if (context == null) {
            context = MonitorContextHolder.pollFallbackContext();
        }
        if (context == null) {
            log.warn("MonitorContext is null in onError; skip metrics for this error");
            return;
        }
        String userId = context.getUserId();
        String sessionId = context.getSessionId();
        // 获取模型名称和错误类型
        String modelName = errorContext.chatRequest().modelName();
        String errorMessage = errorContext.error().getMessage();
        // 记录失败请求
        aiModelMetricsCollector.recordRequest(userId, sessionId, modelName, "error");
        aiModelMetricsCollector.recordError(userId, sessionId, modelName, errorMessage);
        // 记录响应时间（即使是错误响应）
        recordResponseTime(attributes, userId, sessionId, modelName);
    }


    /**
     * 记录响应时间
     */
    private void recordResponseTime(Map<Object, Object> attributes, String userId, String sessionId, String modelName) {
        if (attributes == null) {
            return;
        }
        Instant startTime = (Instant) attributes.get(REQUEST_START_TIME_KEY);
        if (startTime == null) {
            return;
        }
        Duration responseTime = Duration.between(startTime, Instant.now());
        aiModelMetricsCollector.recordResponseTime(userId, sessionId, modelName, responseTime);
    }

    /**
     * 记录Token使用情况
     */
    private void recordTokenUsage(ChatModelResponseContext responseContext, String userId, String sessionId, String modelName) {
        TokenUsage tokenUsage = responseContext.chatResponse().metadata().tokenUsage();
        if (tokenUsage != null) {
            aiModelMetricsCollector.recordTokenUsage(userId, sessionId, modelName, "input", tokenUsage.inputTokenCount());
            aiModelMetricsCollector.recordTokenUsage(userId, sessionId, modelName, "output", tokenUsage.outputTokenCount());
            aiModelMetricsCollector.recordTokenUsage(userId, sessionId, modelName, "total", tokenUsage.totalTokenCount());
        }
    }
}
