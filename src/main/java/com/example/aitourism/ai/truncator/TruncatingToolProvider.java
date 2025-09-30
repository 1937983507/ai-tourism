package com.example.aitourism.ai.truncator;

import com.example.aitourism.ai.truncator.McpResultTruncator;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 截断MCP工具执行的文本结果的ToolProvider包装器
 * 以避免超出模型令牌限制。它通过提供的mcpclient发现工具
 * 并包装每个工具执行器以截断输出。
 */
public class TruncatingToolProvider implements ToolProvider {

    private final List<McpClient> mcpClients;
    private final int maxLength;
    private final Map<String, Integer> toolSpecificLimits;

    public TruncatingToolProvider(List<McpClient> mcpClients, int maxLength, Map<String, Integer> toolSpecificLimits) {
        this.mcpClients = new ArrayList<>(mcpClients);
        this.maxLength = maxLength;
        this.toolSpecificLimits = toolSpecificLimits;
    }

    @Override
    public ToolProviderResult provideTools(ToolProviderRequest request) {
        ToolProviderResult.Builder builder = ToolProviderResult.builder();
        for (McpClient client : mcpClients) {
            List<ToolSpecification> specs = client.listTools();
            for (ToolSpecification spec : specs) {
                builder.add(spec, (executionRequest, memoryId) -> {
                    // 执行工具调用
                    Object raw = client.executeTool((ToolExecutionRequest) executionRequest);
                    String text = raw == null ? "" : String.valueOf(raw);
                    int limit = toolSpecificLimits != null && toolSpecificLimits.containsKey(spec.name())
                            ? toolSpecificLimits.get(spec.name())
                            : maxLength;
                    // 执行内容裁剪
                    return McpResultTruncator.truncateResult(text, limit);
                });
            }
        }
        return builder.build();
    }
}


