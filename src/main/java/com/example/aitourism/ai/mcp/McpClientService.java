package com.example.aitourism.ai.mcp;

import com.example.aitourism.ai.truncator.TruncatingToolProvider;
import com.example.aitourism.config.McpConfig;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.service.tool.ToolProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class McpClientService {

    private final McpConfig mcpConfig;
    
    /**
     * MCP工具结果裁剪的最大长度配置
     * 免费API对模型输入有4096 token上限，这里设置为2000字符作为安全边界
     */
    @Value("${mcp.result-truncation.max-length:2000}")
    private int maxResultLength;
    
    /**
     * 是否启用MCP工具结果裁剪功能
     */
    @Value("${mcp.result-truncation.enabled:true}")
    private boolean truncationEnabled;
    
    /**
     * 工具特定的长度限制配置
     * 格式：maps_weather:500,baidu_search:1500
     */
    @Value("${mcp.result-truncation.tool-specific-limits:}")
    private String toolSpecificLimitsConfig;

    /**
     * 创建MCP工具提供者
     * 如果启用了裁剪功能，则使用TruncatingToolProvider包装原始提供者
     * @return 工具提供者实例
     */
    public ToolProvider createToolProvider() {
        log.info("开始创建MCP工具提供者，裁剪功能: {}, 最大长度: {}", truncationEnabled, maxResultLength);
        
        // 创建MCP客户端列表
        List<McpClient> mcpClients = mcpConfig.getClients().stream()
                .map(this::createMcpClient)
                .collect(Collectors.toList());

        // 如果开启裁剪，则使用自定义的 TruncatingToolProvider 包裹执行结果
        if (truncationEnabled) {
            log.info("启用MCP工具结果裁剪功能，最大长度限制: {} 字符", maxResultLength);
            Map<String, Integer> toolSpecificLimits = parseToolSpecificLimits();
            log.info("工具特定限制: {}", toolSpecificLimits);
            return new TruncatingToolProvider(mcpClients, maxResultLength, toolSpecificLimits);
        }

        // 否则使用基础的 MCP 工具提供者
        return McpToolProvider.builder()
                .mcpClients(mcpClients)
                .build();
    }
    
    /**
     * 解析工具特定的长度限制配置
     * @return 工具名称到长度限制的映射
     */
    private Map<String, Integer> parseToolSpecificLimits() {
        Map<String, Integer> limits = new HashMap<>();
        
        if (toolSpecificLimitsConfig == null || toolSpecificLimitsConfig.trim().isEmpty()) {
            log.debug("未配置工具特定长度限制，使用默认配置");
            return limits;
        }
        
        try {
            // 解析配置字符串，格式：tool1:length1,tool2:length2
            String[] toolConfigs = toolSpecificLimitsConfig.split(",");
            for (String toolConfig : toolConfigs) {
                String[] parts = toolConfig.trim().split(":");
                if (parts.length == 2) {
                    String toolName = parts[0].trim();
                    int maxLength = Integer.parseInt(parts[1].trim());
                    limits.put(toolName, maxLength);
                    log.info("配置工具 {} 的长度限制为: {} 字符", toolName, maxLength);
                } else {
                    log.warn("工具特定长度限制配置格式错误，跳过: {}", toolConfig);
                }
            }
        } catch (Exception e) {
            log.error("解析工具特定长度限制配置时发生错误: {}", e.getMessage(), e);
        }
        
        return limits;
    }

    private McpClient createMcpClient(McpConfig.McpClientConfig config) {
        long timeoutSec = config.getTimeoutSeconds() != null ? config.getTimeoutSeconds() : 600;
        log.info("[MCP] createMcpClient: url={}, timeoutSec={}, logReq={}, logResp={}",
                config.getSseUrl(), timeoutSec, config.isLogRequests(), config.isLogResponses());
        McpTransport transport = new HttpMcpTransport.Builder()
                .sseUrl(config.getSseUrl())
                .timeout(Duration.ofSeconds(timeoutSec))
                .logRequests(config.isLogRequests())
                .logResponses(config.isLogResponses())
                .build();

        // TODO 支持Stdio模式的MCP服务
        // 官方示例： https://docs.langchain4j.info/tutorials/mcp
//        McpTransport transport = new StdioMcpTransport.Builder()
//                .command(List.of("/usr/bin/npm", "exec", "@modelcontextprotocol/server-everything@0.6.2"))
//                .logEvents(true)
//                .build();

        McpClient base = new DefaultMcpClient.Builder()
                .transport(transport)
                .build();
        return base;
    }

    // 轻量心跳：利用 OkHttp HEAD/GET 请求到 MCP SSE 的同域健康路径
    public boolean ping(String url, int timeoutSeconds) {
        log.debug("[MCP] ping start: url={}, timeoutSec={}", url, timeoutSeconds);
        OkHttpClient client = new OkHttpClient.Builder()
                .callTimeout(Duration.ofSeconds(timeoutSeconds))
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .readTimeout(Duration.ofSeconds(timeoutSeconds))
                .writeTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        try (Response resp = client.newCall(request).execute()) {
            boolean ok = resp.isSuccessful();
            log.debug("[MCP] ping done: url={}, code={}, ok={}", url, resp.code(), ok);
            return ok;
        } catch (IOException e) {
            log.warn("[MCP] ping error: url={}, msg={}", url, e.getMessage());
            return false;
        }
    }

    // 遍历配置的所有 MCP 客户端，任意一个可达即返回 true
    public boolean pingAny(int timeoutSeconds) {
        if (mcpConfig.getClients() == null || mcpConfig.getClients().isEmpty()) {
            return false;
        }
        for (McpConfig.McpClientConfig client : mcpConfig.getClients()) {
            if (client.getSseUrl() != null && ping(client.getSseUrl(), timeoutSeconds)) {
                log.info("[MCP] pingAny ok: {}", client.getSseUrl());
                return true;
            }
        }
        log.warn("[MCP] pingAny failed: all MCP endpoints unreachable");
        return false;
    }
}