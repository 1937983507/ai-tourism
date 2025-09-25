package com.example.aitourism.service.impl;

import com.example.aitourism.entity.Message;
import com.example.aitourism.entity.Session;
import com.example.aitourism.mapper.ChatMessageMapper;
import com.example.aitourism.mapper.SessionMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.*;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.TokenStream;
//import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.memory.ChatMemoryAccess;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import dev.langchain4j.model.chat.StreamingChatModel;

import dev.langchain4j.data.message.ChatMessage;

import com.example.aitourism.service.Assistant;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
//@Lazy
//@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class AssistantService {

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.base-url}")
    private String baseUrl;

    @Value("${openai.model-name}")
    private String modelName;

    @Value("${openai.max-output-tokens:800}")
    private Integer maxOutputTokens;

    @Value("${mcp.max-history-messages:6}")
    private Integer maxHistoryMessages;

    private final McpClientService mcpClientService;

    private Assistant assistant;

    @PostConstruct
    public void init_stream() {

        log.info("开始初始化 init_stream ");

//        // 手动管理记忆
//        ChatMemoryStoreService store = new ChatMemoryStoreService();
//        ChatMemoryProvider chatMemoryProvider = memoryId -> MessageWindowChatMemory.builder()
//                .id(memoryId)
//                .maxMessages(10)
//                .chatMemoryStore(store)
//                .build();

        StreamingChatModel streamingModel = OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .maxTokens(maxOutputTokens)
                .build();
        log.info("创建model成功");

        try {
            assistant = AiServices.builder(Assistant.class)
                    .streamingChatModel(streamingModel)
                    .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(maxHistoryMessages))
//                    .chatMemoryProvider(chatMemoryProvider)
                    .toolProvider(mcpClientService.createToolProvider())
                    .build();
            log.info("创建assistant成功");
        } catch (Exception e) {
            // 可以在这里添加重试逻辑
            log.error("创建assistance失败："+e.getMessage());
            throw new RuntimeException("创建assistance失败：", e);
        }
    }

    // 简单心跳：周期性探测 MCP 服务器，失败则重建 Assistant
    @Scheduled(fixedDelayString = "${mcp.heartbeat-interval-seconds:300000}")
    public void heartbeat() {
        try {
            if (mcpClientService == null) return;
            if (assistant == null) {
                init_stream();
                return;
            }
        } catch (Exception e) {
            init_stream();
        }
    }

    /**
     * 确保 Assistant 与 MCP 工具可用；不可用则尝试重建。
     * 返回 true 表示已就绪。
     */
    public boolean ensureReady() {
        try {
            if (assistant == null) {
                log.warn("ensureReady: assistant is null, re-init");
                init_stream();
            }
            // 轻量探测 MCP 可用性
            int timeout = 5;
            try {
                timeout = Math.max(1, Integer.parseInt(System.getProperty("mcp.ping-timeout-seconds", "5")));
            } catch (Exception ignore) {}
            boolean ok = mcpClientService.pingAny(timeout);
            if (!ok) {
                log.warn("ensureReady: MCP ping failed, re-init and retry");
                init_stream();
                ok = mcpClientService.pingAny(timeout);
            }
            log.info("ensureReady result: assistantPresent={}, mcpOk={}", assistant != null, ok);
            return ok && assistant != null;
        } catch (Exception e) {
            log.error("ensureReady exception: {}", e.getMessage(), e);
            return false;
        }
    }

    public TokenStream chat_Stream(String memoryId, String message) {
        // 延迟初始化，确保在第一次使用时创建
        // if (assistant == null) {
        //       init_stream();
        // }
       // 每一次请求都创建
       init_stream();

        try {
            log.info("开始向大模型发起请求，进行旅游规划");
            // 开始发起流式请求
            return assistant.chat_Stream(memoryId, message);
        } catch (Exception e) {
            // 可以在这里添加重试逻辑
            log.error("大模型请求报错：" + e.getMessage(), e);
            throw new RuntimeException("Chat service unavailable", e);
        }
    }



}