package com.example.aitourism.service;

import com.example.aitourism.config.AbTestConfig;
import com.example.aitourism.monitor.AiModelMetricsCollector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Random;

/**
 * A/B测试服务
 * 用于控制不同优化功能的启用状态，并记录对比数据
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AbTestService {
    
    private final AbTestConfig abTestConfig;
    private final AiModelMetricsCollector metricsCollector;
    private final Random random = new Random();
    
    /**
     * 判断是否应该使用工具调用缓存
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @return true表示使用缓存，false表示不使用缓存
     */
    public boolean shouldUseToolCache(String userId, String sessionId) {
        // if (!abTestConfig.getToolCache().isEnabled()) {
        //     return false;
        // }
        
        // // 根据配置的无缓存测试比例决定是否使用缓存
        // double noCacheRatio = abTestConfig.getToolCache().getNoCacheTestRatio();
        // boolean useCache = random.nextDouble() >= noCacheRatio;
        
        // log.debug("工具缓存A/B测试 - 用户: {}, 会话: {}, 使用缓存: {}", userId, sessionId, useCache);
        // return useCache;

        return true;
    }
    
    /**
     * 判断是否应该使用AI服务实例缓存
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @return true表示使用缓存，false表示不使用缓存
     */
    public boolean shouldUseServiceCache(String userId, String sessionId) {
        // if (!abTestConfig.getServiceCache().isEnabled()) {
        //     return false;
        // }
        
        // double noCacheRatio = abTestConfig.getServiceCache().getNoCacheTestRatio();
        // boolean useCache = random.nextDouble() >= noCacheRatio;
        
        // log.debug("服务缓存A/B测试 - 用户: {}, 会话: {}, 使用缓存: {}", userId, sessionId, useCache);
        // return useCache;

        return true;
    }
    
    /**
     * 判断是否应该使用MCP结果裁剪
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @return true表示使用裁剪，false表示不使用裁剪
     */
    public boolean shouldUseTruncation(String userId, String sessionId) {
        // if (!abTestConfig.getTruncation().isEnabled()) {
        //     return false;
        // }
        
        // double noTruncationRatio = abTestConfig.getTruncation().getNoTruncationTestRatio();
        // boolean useTruncation = random.nextDouble() >= noTruncationRatio;
        
        // log.debug("结果裁剪A/B测试 - 用户: {}, 会话: {}, 使用裁剪: {}", userId, sessionId, useTruncation);
        // return useTruncation;

        return true;
    }
    
    /**
     * 记录工具调用性能对比数据
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @param toolName 工具名称
     * @param duration 响应时间
     * @param fromCache 是否来自缓存
     */
    public void recordToolPerformance(String userId, String sessionId, String toolName, 
                                    Duration duration, boolean fromCache) {
        // 记录响应时间
        metricsCollector.recordToolResponseTime(userId, sessionId, toolName, duration, fromCache);
        
        // 记录缓存命中/未命中
        if (fromCache) {
            metricsCollector.recordToolCacheHit(userId, sessionId, toolName);
        } else {
            metricsCollector.recordToolCacheMiss(userId, sessionId, toolName);
        }
    }
    
    /**
     * 记录AI服务实例创建性能对比数据
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @param duration 创建时间
     * @param fromCache 是否来自缓存
     */
    public void recordServiceCreationPerformance(String userId, String sessionId, 
                                               Duration duration, boolean fromCache) {
        metricsCollector.recordServiceCreationTime(userId, sessionId, duration, fromCache);
    }
    
    /**
     * 记录MCP结果裁剪对比数据
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @param toolName 工具名称
     * @param beforeTokens 裁剪前Token数
     * @param afterTokens 裁剪后Token数
     */
    public void recordTruncationPerformance(String userId, String sessionId, String toolName,
                                          long beforeTokens, long afterTokens) {
        metricsCollector.recordTruncation(userId, sessionId, toolName, beforeTokens, afterTokens);
    }
    
    /**
     * 获取工具缓存命中率
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @param toolName 工具名称
     * @return 命中率（0.0-1.0）
     */
    public double getToolCacheHitRate(String userId, String sessionId, String toolName) {
        try {
            // 这里应该从Prometheus查询，但为了简化，我们使用配置值
            // 实际生产环境中应该调用Prometheus API
            log.debug("获取工具缓存命中率 - 用户: {}, 会话: {}, 工具: {}", userId, sessionId, toolName);
            
            // 返回一个基于配置的估算值
            return abTestConfig.getToolCache().getTargetHitRate();
        } catch (Exception e) {
            log.warn("获取工具缓存命中率失败: {}", e.getMessage());
            return 0.0;
        }
    }
    
    /**
     * 获取性能提升比例
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @param toolName 工具名称
     * @return 性能提升比例（0.0-1.0）
     */
    public double getPerformanceImprovement(String userId, String sessionId, String toolName) {
        try {
            // 这里应该从Prometheus查询缓存和非缓存的平均响应时间
            // 然后计算性能提升比例
            log.debug("获取性能提升比例 - 用户: {}, 会话: {}, 工具: {}", userId, sessionId, toolName);
            
            // 基于经验值返回一个估算的性能提升比例
            // 实际生产环境中应该查询Prometheus获取真实数据
            return 0.3; // 假设缓存能带来30%的性能提升
        } catch (Exception e) {
            log.warn("获取性能提升比例失败: {}", e.getMessage());
            return 0.0;
        }
    }
}
