package com.example.aitourism.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * A/B测试配置类
 * 用于控制不同优化功能的启用状态，便于进行性能对比测试
 */
@Component
@ConfigurationProperties(prefix = "ab-test")
@Data
public class AbTestConfig {
    
    /**
     * 工具调用缓存A/B测试配置
     */
    private ToolCacheAbTest toolCache = new ToolCacheAbTest();
    
    /**
     * AI服务实例缓存A/B测试配置
     */
    private ServiceCacheAbTest serviceCache = new ServiceCacheAbTest();
    
    /**
     * MCP结果裁剪A/B测试配置
     */
    private TruncationAbTest truncation = new TruncationAbTest();
    
    @Data
    public static class ToolCacheAbTest {
        /**
         * 是否启用工具调用缓存
         */
        private boolean enabled = true;
        
        /**
         * 缓存命中率目标（用于验证缓存效果）
         */
        private double targetHitRate = 0.8;
        
        /**
         * 是否记录无缓存对比数据
         */
        private boolean recordNoCacheBaseline = true;
        
        /**
         * 无缓存测试比例（0.0-1.0，用于A/B测试）
         */
        private double noCacheTestRatio = 0.1;
    }
    
    @Data
    public static class ServiceCacheAbTest {
        /**
         * 是否启用AI服务实例缓存
         */
        private boolean enabled = true;
        
        /**
         * 是否记录无缓存对比数据
         */
        private boolean recordNoCacheBaseline = true;
        
        /**
         * 无缓存测试比例
         */
        private double noCacheTestRatio = 0.05;
    }
    
    @Data
    public static class TruncationAbTest {
        /**
         * 是否启用MCP结果裁剪
         */
        private boolean enabled = true;
        
        /**
         * 是否记录无裁剪对比数据
         */
        private boolean recordNoTruncationBaseline = true;
        
        /**
         * 无裁剪测试比例
         */
        private double noTruncationTestRatio = 0.05;
    }
}

