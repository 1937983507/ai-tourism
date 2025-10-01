package com.example.aitourism.monitor;

import org.springframework.stereotype.Component;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.time.Duration;

// 指标收集器
@Component
@Slf4j
public class AiModelMetricsCollector {

    @Resource
    private MeterRegistry meterRegistry;

    // 缓存已创建的指标，避免重复创建（按指标类型分离缓存）
    private final ConcurrentMap<String, Counter> requestCountersCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Counter> errorCountersCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Counter> tokenCountersCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Timer> responseTimersCache = new ConcurrentHashMap<>();
    
    // 新增：A/B测试和性能对比相关指标缓存
    private final ConcurrentMap<String, Counter> cacheHitCountersCache = new ConcurrentHashMap<>();  // 工具调用命中缓存的计数
    private final ConcurrentMap<String, Counter> cacheMissCountersCache = new ConcurrentHashMap<>();  // 工具调用未命中缓存的计数
    private final ConcurrentMap<String, Timer> toolResponseTimersCache = new ConcurrentHashMap<>();  // 工具调用，在有无缓存的情况下，的响应时间
    private final ConcurrentMap<String, Timer> serviceCreationTimersCache = new ConcurrentHashMap<>();  // AI服务实例，在有无缓存下的情况下，的创建时间
    private final ConcurrentMap<String, Counter> truncationCountersCache = new ConcurrentHashMap<>();  // TODO 这个待更新

    /**
     * 记录请求次数
     */
    public void recordRequest(String userId, String sessionId, String modelName, String status) {
        String key = String.format("%s_%s_%s_%s", userId, sessionId, modelName, status);
        Counter counter = requestCountersCache.computeIfAbsent(key, k ->
                Counter.builder("ai_model_requests_total")
                        .description("AI模型总请求次数")
                        .tag("user_id", userId)
                        .tag("session_id", sessionId)
                        .tag("model_name", modelName)
                        .tag("status", status)
                        .register(meterRegistry)
        );
        counter.increment();
    }

    /**
     * 记录错误
     */
    public void recordError(String userId, String sessionId, String modelName, String errorMessage) {
        String key = String.format("%s_%s_%s_%s", userId, sessionId, modelName, errorMessage);
        Counter counter = errorCountersCache.computeIfAbsent(key, k ->
                Counter.builder("ai_model_errors_total")
                        .description("AI模型错误次数")
                        .tag("user_id", userId)
                        .tag("session_id", sessionId)
                        .tag("model_name", modelName)
                        .tag("error_message", errorMessage)
                        .register(meterRegistry)
        );
        counter.increment();
    }

    /**
     * 记录Token消耗
     */
    public void recordTokenUsage(String userId, String sessionId, String modelName,
                                 String tokenType, long tokenCount) {
        String key = String.format("%s_%s_%s_%s", userId, sessionId, modelName, tokenType);
        Counter counter = tokenCountersCache.computeIfAbsent(key, k ->
                Counter.builder("ai_model_tokens_total")
                        .description("AI模型Token消耗总数")
                        .tag("user_id", userId)
                        .tag("session_id", sessionId)
                        .tag("model_name", modelName)
                        .tag("token_type", tokenType)
                        .register(meterRegistry)
        );
        counter.increment(tokenCount);
    }

    /**
     * 记录模型的响应时间
     */
    public void recordResponseTime(String userId, String sessionId, String modelName, Duration duration) {
        String key = String.format("%s_%s_%s", userId, sessionId, modelName);
        Timer timer = responseTimersCache.computeIfAbsent(key, k ->
                Timer.builder("ai_model_response_duration_seconds")
                        .description("AI模型响应时间")
                        .tag("user_id", userId)
                        .tag("session_id", sessionId)
                        .tag("model_name", modelName)
                        .register(meterRegistry)
        );
        timer.record(duration);
    }

    /**
     * 记录工具调用缓存命中
     */
    public void recordToolCacheHit(String userId, String sessionId, String toolName) {
        String key = String.format("%s_%s_%s", userId, sessionId, toolName);
        Counter counter = cacheHitCountersCache.computeIfAbsent(key, k ->
                Counter.builder("ai_tool_cache_hits_total")
                        .description("工具调用缓存命中次数")
                        .tag("user_id", userId)
                        .tag("session_id", sessionId)
                        .tag("tool_name", toolName)
                        .register(meterRegistry)
        );
        counter.increment();
    }

    /**
     * 记录工具调用缓存未命中
     */
    public void recordToolCacheMiss(String userId, String sessionId, String toolName) {
        String key = String.format("%s_%s_%s", userId, sessionId, toolName);
        Counter counter = cacheMissCountersCache.computeIfAbsent(key, k ->
                Counter.builder("ai_tool_cache_misses_total")
                        .description("工具调用缓存未命中次数")
                        .tag("user_id", userId)
                        .tag("session_id", sessionId)
                        .tag("tool_name", toolName)
                        .register(meterRegistry)
        );
        counter.increment();
    }

    /**
     * 记录工具调用响应时间（用于对比有无缓存的性能差异）
     */
    public void recordToolResponseTime(String userId, String sessionId, String toolName, 
                                     Duration duration, boolean fromCache) {
        String key = String.format("%s_%s_%s_%s", userId, sessionId, toolName, fromCache ? "cache" : "nocache");
        Timer timer = toolResponseTimersCache.computeIfAbsent(key, k ->
                Timer.builder("ai_tool_response_duration_seconds")
                        .description("工具调用响应时间")
                        .tag("user_id", userId)
                        .tag("session_id", sessionId)
                        .tag("tool_name", toolName)
                        .tag("from_cache", String.valueOf(fromCache))
                        .register(meterRegistry)
        );
        timer.record(duration);
    }

    /**
     * 记录AI服务实例创建时间
     */
    public void recordServiceCreationTime(String userId, String sessionId, Duration duration, boolean fromCache) {
        String key = String.format("%s_%s_%s", userId, sessionId, fromCache ? "cache" : "nocache");
        Timer timer = serviceCreationTimersCache.computeIfAbsent(key, k ->
                Timer.builder("ai_service_creation_duration_seconds")
                        .description("AI服务实例创建时间")
                        .tag("user_id", userId)
                        .tag("session_id", sessionId)
                        .tag("from_cache", String.valueOf(fromCache))
                        .register(meterRegistry)
        );
        timer.record(duration);
    }

    /**
     * TODO：记录MCP结果裁剪统计，待更新
     */
    public void recordTruncation(String userId, String sessionId, String toolName, 
                               long beforeTokens, long afterTokens) {
        String key = String.format("%s_%s_%s", userId, sessionId, toolName);
        Counter counter = truncationCountersCache.computeIfAbsent(key, k ->
                Counter.builder("ai_tool_truncation_tokens_saved_total")
                        .description("工具结果裁剪节省的Token数量")
                        .tag("user_id", userId)
                        .tag("session_id", sessionId)
                        .tag("tool_name", toolName)
                        .register(meterRegistry)
        );
        counter.increment(beforeTokens - afterTokens);
    }
}
