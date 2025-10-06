package com.example.aitourism.ai;

import com.example.aitourism.ai.guardrail.PromptSafetyInputGuardrail;
import com.example.aitourism.ai.mcp.McpClientService;
import com.example.aitourism.ai.tool.ToolManager;
import com.example.aitourism.exception.InputValidationException;
import com.example.aitourism.mapper.ChatMessageMapper;
import com.example.aitourism.monitor.AiModelMonitorListener;
import com.example.aitourism.monitor.MonitorContext;
import com.example.aitourism.monitor.MonitorContextHolder;
import com.example.aitourism.service.AbTestService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.time.Instant;
import reactor.core.publisher.Flux;

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
    
    // MCP工具结果裁剪的最大长度配置
    @Value("${mcp.result-truncation.max-length:2000}")
    private int maxMcpResultLength;
    
    // 是否启用MCP工具结果裁剪功能
    @Value("${mcp.result-truncation.enabled:true}")
    private boolean mcpTruncationEnabled;

    private final McpClientService mcpClientService;
    private final ChatMemoryStore chatMemoryStore;
    
    private final ChatMessageMapper chatMessageMapper;
    private final ToolManager toolManager;

    @Resource
    private AiModelMonitorListener aiModelMonitorListener;
    
    @Resource
    private AbTestService abTestService;   


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
        
        // 检查是否应该使用缓存（A/B测试）
        boolean shouldUseCache = abTestService.shouldUseServiceCache(userId, sessionId);
        
        // 不允许使用缓存的情况
        if (!shouldUseCache) {
            log.info("A/B测试：跳过缓存，直接创建新实例");
            Instant startTime = Instant.now();
            AssistantService service = createAssistantService(sessionId, userId);
            // 记录无缓存创建时间（仅包含创建本身）
            Duration creationTime = Duration.between(startTime, Instant.now());
            log.info("A/B测试：记录无缓存情况下，AI服务的创建时间为{}", creationTime);
            abTestService.recordServiceCreationPerformance(userId, sessionId, creationTime, false);
            return service;
        }
        
        // 允许使用缓存的情况
        // 先尝试直接命中缓存（只统计获取时间，不包含创建时间）
        Instant lookupStart = Instant.now();
        AssistantService cached = serviceCache.getIfPresent(cacheKey);
        if (cached != null) {
            Duration lookupTime = Duration.between(lookupStart, Instant.now());
            log.info("A/B测试：缓存命中，记录获取耗时 {}", lookupTime);
            // fromCache=true 表示来自缓存；这里统计的是获取时间
            abTestService.recordServiceCreationPerformance(userId, sessionId, lookupTime, true);
            return cached;
        }

        // 缓存未命中：在加载器内部仅统计创建时间（不包含查找时间）
        return serviceCache.get(cacheKey, key -> {
            log.info("A/B测试：缓存未命中，开始创建新实例");
            Instant createStart = Instant.now();
            AssistantService service = createAssistantService(sessionId, userId);
            Duration creationTime = Duration.between(createStart, Instant.now());
            log.info("A/B测试：记录有缓存（未命中->创建）情况下的创建时间为{}", creationTime);
            abTestService.recordServiceCreationPerformance(userId, sessionId, creationTime, true);
            return service;
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
                .listeners(List.of(aiModelMonitorListener))  // 注册监听器
                .build();
        
        log.info("创建流式模型成功");

        // 构建AI服务
        try {
            AssistantService assistantService = AiServices.builder(AssistantService.class)
                    .streamingChatModel(streamingModel)
                    .tools((Object[]) toolManager.getAllTools())
                    // .tools(new WeatherTool())
                    // .toolProvider(mcpClientService.createToolProvider())
                    .chatMemoryProvider(chatMemoryProvider::apply)
                    .maxSequentialToolsInvocations(1)  // 最多连续调用 1 次工具
                    .inputGuardrails(new PromptSafetyInputGuardrail())  // 输入护轨
                    // .outputGuardrails(new RetryOutputGuardrail())  // 输出护轨
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
    public Flux<String> chatStream(String sessionId, String userId, String message) {
        log.info("开始流式对话，会话ID: {}, 用户ID: {}, 消息: {}", sessionId, userId, message);


        // 设置监控上下文
        MonitorContextHolder.setContext(
                MonitorContext.builder()
                        .userId(userId)
                        .sessionId(sessionId)
                        .build()
        );

        // 对话前，首先去获取一下caffeine尝试获取AI Service实例，若是获取不到，则新建实例
        AssistantService assistantService = getAssistantService(sessionId, userId);
        // log.info("获取或创建会话隔离的AI服务成功");
        // log.info(assistantService.toString());
        String memoryId = sessionId;

        try {
            log.info("开始向大模型发起请求，进行旅游规划");
            // 开始发起流式请求
            return assistantService.chat_Stream(memoryId, message);
        } catch (Exception e) {
            // 捕获输入校验相关异常，抛出自定义异常
            String msg = e.getMessage();
            log.error("大模型请求报错：" + e.getMessage(), e);
            if (msg != null && (
                    msg.contains("输入包含不当内容") ||
                            msg.contains("输入内容过长") ||
                            msg.contains("输入内容不能为空") ||
                            msg.contains("检测到恶意输入")
            )) {
                throw new InputValidationException(msg);
            }
            throw new RuntimeException("聊天服务不可用", e);
        } finally {
            // 清除监控上下文
            MonitorContextHolder.clearContext();
        }
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
