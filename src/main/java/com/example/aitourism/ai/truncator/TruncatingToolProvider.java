package com.example.aitourism.ai.truncator;

import com.example.aitourism.ai.truncator.McpResultTruncator;
import com.example.aitourism.service.AbTestService;
import com.example.aitourism.monitor.MonitorContextHolder;
import com.example.aitourism.monitor.MonitorContext;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// TODO 监控部分还需要完善一下逻辑

/**
 * 截断MCP工具执行的文本结果的ToolProvider包装器
 * 以避免超出模型token限制。它通过提供的mcpclient发现工具
 * 并包装每个工具执行器以截断输出。
 */
@Component
@Slf4j
public class TruncatingToolProvider implements ToolProvider {

    private List<McpClient> mcpClients;
    private int maxLength;
    private Map<String, Integer> toolSpecificLimits;
    
    @Autowired
    private AbTestService abTestService;

    public TruncatingToolProvider() {
        // 默认构造函数，用于Spring依赖注入
    }

    public TruncatingToolProvider(List<McpClient> mcpClients, int maxLength, Map<String, Integer> toolSpecificLimits) {
        this.mcpClients = new ArrayList<>(mcpClients);
        this.maxLength = maxLength;
        this.toolSpecificLimits = toolSpecificLimits;
    }
    

    @Override
    public ToolProviderResult provideTools(ToolProviderRequest request) {
        ToolProviderResult.Builder builder = ToolProviderResult.builder();
        
        // 空指针检查
        if (mcpClients == null || mcpClients.isEmpty()) {
            log.warn("MCP客户端列表为空，无法提供工具");
            return builder.build();
        }
        
        for (McpClient client : mcpClients) {
            List<ToolSpecification> specs = client.listTools();
            for (ToolSpecification spec : specs) {
                builder.add(spec, (executionRequest, memoryId) -> {
                    // 执行工具调用
                    Object raw = client.executeTool((ToolExecutionRequest) executionRequest);
                    String text = raw == null ? "" : String.valueOf(raw);
                    
                    // 获取监控上下文
                    MonitorContext context = MonitorContextHolder.getContext();
                    String userId = context != null ? context.getUserId() : "unknown";
                    String sessionId = context != null ? context.getSessionId() : "unknown";
                    
                    // 检查是否应该使用裁剪（A/B测试）
                    boolean shouldTruncate = true;
                    if (abTestService != null) {
                        try {
                            shouldTruncate = abTestService.shouldUseTruncation(userId, sessionId);
                        } catch (Exception e) {
                            log.warn("A/B测试检查失败，使用默认裁剪策略: {}", e.getMessage());
                        }
                    }
                    
                    if (!shouldTruncate) {
                        // A/B测试：不使用裁剪，记录原始Token数
                        long originalTokens = estimateTokenCount(text);
                        if (abTestService != null) {
                            try {
                                abTestService.recordTruncationPerformance(userId, sessionId, spec.name(), 
                                    originalTokens, originalTokens);
                            } catch (Exception e) {
                                log.warn("记录裁剪性能数据失败: {}", e.getMessage());
                            }
                        }
                        return text;
                    }
                    
                    // 正常裁剪流程
                    int limit = toolSpecificLimits != null && toolSpecificLimits.containsKey(spec.name())
                            ? toolSpecificLimits.get(spec.name())
                            : maxLength;
                    
                    String truncatedText = McpResultTruncator.truncateResult(text, limit);
                    
                    // 记录裁剪效果
                    long beforeTokens = estimateTokenCount(text);
                    long afterTokens = estimateTokenCount(truncatedText);
                    if (abTestService != null) {
                        try {
                            abTestService.recordTruncationPerformance(userId, sessionId, spec.name(), 
                                beforeTokens, afterTokens);
                        } catch (Exception e) {
                            log.warn("记录裁剪性能数据失败: {}", e.getMessage());
                        }
                    }
                    
                    return truncatedText;
                });
            }
        }
        return builder.build();
    }
    
    /**
     * 估算文本的Token数量（简单估算：1个中文字符≈1.5个Token，1个英文字符≈0.75个Token）
     */
    private long estimateTokenCount(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        
        // 简单估算：中文字符数 * 1.5 + 英文字符数 * 0.75
        long chineseChars = text.chars().filter(ch -> ch >= 0x4E00 && ch <= 0x9FFF).count();
        long otherChars = text.length() - chineseChars;
        
        return Math.round(chineseChars * 1.5 + otherChars * 0.75);
    }
    
    // Setter方法，用于Spring依赖注入
    public void setMcpClients(List<McpClient> mcpClients) {
        this.mcpClients = mcpClients != null ? new ArrayList<>(mcpClients) : new ArrayList<>();
    }
    
    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
    }
    
    public void setToolSpecificLimits(Map<String, Integer> toolSpecificLimits) {
        this.toolSpecificLimits = toolSpecificLimits;
    }
}


