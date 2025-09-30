package com.example.aitourism.ai;

import com.example.aitourism.ai.guardrail.PromptSafetyInputGuardrail;
import com.example.aitourism.ai.mcp.McpClientService;
import com.example.aitourism.ai.tool.WeatherTool;
import com.example.aitourism.mapper.ChatMessageMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 会话隔离的AI助手服务工厂
 * 每个会话都有独立的AI服务实例和记忆空间
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MemoryAssistantServiceFactory {

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.base-url}")
    private String baseUrl;

    @Value("${openai.model-name}")
    private String modelName;

    @Value("${openai.max-output-tokens:800}")
    private Integer maxOutputTokens;

    @Value("${mcp.max-history-messages:20}")
    private Integer maxHistoryMessages;
    
    @Value("${mcp.result-truncation.max-length:2000}")
    private int maxMcpResultLength;
    
    @Value("${mcp.result-truncation.enabled:true}")
    private boolean mcpTruncationEnabled;

    private final McpClientService mcpClientService;
    private final ChatMemoryStore chatMemoryStore;
    
    private final ChatMessageMapper chatMessageMapper;


    /**
     * 基于 caffeine 实现的AI服务实例缓存 - 按会话隔离
     * 缓存策略：
     */
    private final Cache<String, AssistantService> serviceCache = Caffeine.newBuilder()
            .maximumSize(100)  // 最大缓存 100 个实例
            .expireAfterWrite(Duration.ofMinutes(60))  // 写入后 60 分钟过期
            .expireAfterAccess(Duration.ofMinutes(60))  // 访问后 30 分钟过期
            .removalListener((key, value, cause) -> {
                log.debug("AI服务实例被移除，会话: {}, 原因: {}", key, cause);
            })
            .build();



    /**
     * 获取或创建会话隔离的 AI 服务实例。
     * 使用 Caffeine 基于会话键进行缓存，避免重复创建模型与记忆。
     */
    public AssistantService getAssistantService(String sessionId, String userId) {
        log.info("获取或创建会话隔离的AI服务");
        String cacheKey = sessionId;
        
        return serviceCache.get(cacheKey, key -> {
            // 基于 cacheKey 查找缓存，若是缓存不存在则生成缓存元素。
            return createAssistantService(sessionId, userId);
        });
    }


    /**
     * 按会话构建新的 AI 服务实例。
     * - 通过 chatMemoryProvider 按 @MemoryId（sessionId）提供 MessageWindowChatMemory
     * - 在 provider 内部：
     *   1) 使用通用 ChatMemoryStore（官方 Redis 或自定义 Redis）进行持久化
     *   2) 从 MySQL 读取历史消息并预热到记忆中
     */
    private AssistantService createAssistantService(String sessionId, String userId) {
        
        // 验证参数
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new IllegalArgumentException("sessionId 不能为空");
        }
        
        // 使用sessionId作为唯一键，Redis会自动加前缀
        String memoryId = sessionId;

        log.info("开始创建对话记忆，memoryId: {}, chatMemoryStore 类型: {}", 
                memoryId, chatMemoryStore.getClass().getSimpleName());

        // 使用 Provider 按 memoryId 构建记忆，兼容 @MemoryId
        java.util.function.Function<Object, MessageWindowChatMemory> chatMemoryProvider = idObj -> {
            String id = String.valueOf(idObj);
            MessageWindowChatMemory m = MessageWindowChatMemory
                    .builder()
                    .id(id)
                    .chatMemoryStore(chatMemoryStore)  
                    .maxMessages(maxHistoryMessages)   // 最大消息数量
                    .build();
            
            // 从 MySQL 中回填消息到Redis中
            try {
                loadChatHistoryToMemory(sessionId, m);
            } catch (Exception e) {
                log.warn("根据 memoryId 预加载历史失败: {}", e.getMessage());
            }
            
            return m;
        };

        // 创建流式模型
        OpenAiStreamingChatModel streamingModel = OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .maxTokens(maxOutputTokens)
                .build();
        
        log.info("创建流式模型成功");

        // 构建AI服务
        try {
            AssistantService assistantService = AiServices.builder(AssistantService.class)
                    .streamingChatModel(streamingModel)
                     .tools(new WeatherTool())
                    .toolProvider(mcpClientService.createToolProvider())
                    .chatMemoryProvider(chatMemoryProvider::apply)
                    .inputGuardrails(new PromptSafetyInputGuardrail())
                    .build();
            
            log.info("AI服务构建成功，记忆存储类型: {}", chatMemoryStore.getClass().getSimpleName());
            return assistantService;
        } catch (Exception e) {
            log.error("AI服务构建失败", e);
            throw new RuntimeException("AI服务初始化失败", e);
        }
    }


    /**
     * 发起流式对话：
     * - 通过缓存获取或创建会话级 AssistantService
     * - 以 sessionId 作为 @MemoryId，保障会话级隔离
     */
    public TokenStream chatStream(String sessionId, String userId, String message) {
        log.info("开始流式对话，会话ID: {}, 用户ID: {}, 消息: {}", sessionId, userId, message);
        AssistantService assistantService = getAssistantService(sessionId, userId);
        log.info("获取或创建会话隔离的AI服务成功");
        log.info(assistantService.toString());
        String memoryId = sessionId;
        return assistantService.chat_Stream(memoryId, message);
    }


    /**
     * 清除指定会话的AI服务缓存
     */
    public void clearSessionCache(String sessionId, String userId) {
        // String cacheKey = buildCacheKey(sessionId, userId);
        String cacheKey = sessionId;
        serviceCache.invalidate(cacheKey);
        log.info("清除会话 {} 用户 {} 的AI服务缓存", sessionId, userId);
    }


    /**
     * 清除用户所有会话的AI服务缓存
     */
    public void clearUserCache(String userId) {
        // 这里可以根据需要实现清除用户所有会话的逻辑
        log.info("清除用户 {} 的所有AI服务缓存", userId);
    }


    /**
     * 将 MySQL 中的历史消息预加载到当前记忆窗口。
     * 注意：只做预热，不改变数据库内容；增量写入由业务层或 LangChain4j 负责。
     */
    private void loadChatHistoryToMemory(String sessionId, MessageWindowChatMemory chatMemory) {
        try {
            var dbMessages = chatMessageMapper.findBySessionId(sessionId);
            if (dbMessages == null || dbMessages.isEmpty()) {
                log.debug("为会话 {} 没有历史对话", sessionId);
                return;
            }
            chatMemory.clear();
            for (var dbMessage : dbMessages) {
                switch (dbMessage.getRole().toLowerCase()) {
                    // 将数据库中读取到的消息，按照消息角色 创建不同的 Message 存入到 memory 中
                    case "user":
                        chatMemory.add(dev.langchain4j.data.message.UserMessage.from(dbMessage.getContent()));
                        break;
                    case "assistant":
                        chatMemory.add(dev.langchain4j.data.message.AiMessage.from(dbMessage.getContent()));
                        break;
                    case "system":
                        chatMemory.add(dev.langchain4j.data.message.SystemMessage.from(dbMessage.getContent()));
                        break;
                    default:
                        chatMemory.add(dev.langchain4j.data.message.UserMessage.from(dbMessage.getContent()));
                        break;
                }
            }
            log.info("为会话 {} 预加载 {} 条历史消息到记忆", sessionId, dbMessages.size());
        } catch (Exception e) {
            log.error("加载历史对话失败，会话: {}, error: {}", sessionId, e.getMessage(), e);
        }
    }

    


    /**
     * 简单心跳：周期性探测MCP服务器
     */
    @Scheduled(fixedDelayString = "${mcp.heartbeat-interval-seconds:300000}")
    public void heartbeat() {
        try {
            if (mcpClientService == null) return;
            // 这里可以添加MCP健康检查逻辑
        } catch (Exception e) {
            log.error("MCP心跳检查失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 确保AI服务可用
     */
    public boolean ensureReady() {
        try {
            // 这里可以添加服务可用性检查逻辑
            return true;
        } catch (Exception e) {
            log.error("AI服务可用性检查失败: {}", e.getMessage(), e);
            return false;
        }
    }
}
