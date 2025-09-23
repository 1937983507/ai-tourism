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

    @Data
    public static class McpClientConfig {
        private String name;
        private String sseUrl;
        private boolean logRequests;
        private boolean logResponses;
    }
}