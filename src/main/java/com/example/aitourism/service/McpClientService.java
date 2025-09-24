package com.example.aitourism.service;

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

@Service
@RequiredArgsConstructor
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
        McpTransport transport = new HttpMcpTransport.Builder()
                .sseUrl(config.getSseUrl())
                .timeout(Duration.ofSeconds(60))
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
}