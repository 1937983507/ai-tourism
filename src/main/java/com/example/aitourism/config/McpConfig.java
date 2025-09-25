package com.example.aitourism.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "mcp")
public class McpConfig {
    private List<McpClientConfig> clients;
    // 可选：心跳与全局超时配置
    private boolean heartbeatEnabled = true;
    private int heartbeatIntervalSeconds = 300; // 5分钟
    private int pingTimeoutSeconds = 5; // 心跳单次请求超时

    @Data
    public static class McpClientConfig {
        private String name;
        private String sseUrl;
        private boolean logRequests;
        private boolean logResponses;
        // 传输层超时（秒），覆盖默认
        private Integer timeoutSeconds;
    }
}