package com.example.aitourism.service.impl;

import com.example.aitourism.config.McpConfig;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.service.tool.ToolProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
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

    public ToolProvider createToolProvider() {
        List<McpClient> mcpClients = mcpConfig.getClients().stream()
                .map(this::createMcpClient)
                .collect(Collectors.toList());

        return McpToolProvider.builder()
                .mcpClients(mcpClients)
                .build();
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


        return new DefaultMcpClient.Builder()
                .transport(transport)
                .build();
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