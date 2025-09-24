package com.example.aitourism.service;

import ch.qos.logback.classic.Logger;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import org.springframework.web.context.WebApplicationContext;
import dev.langchain4j.model.chat.StreamingChatModel;

@Service
@RequiredArgsConstructor
@Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class AssistantService {

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.base-url}")
    private String baseUrl;

    @Value("${openai.model-name}")
    private String modelName;

    private final McpClientService mcpClientService;

    private Assistant assistant;

    @PostConstruct
    public void init() {
        OpenAiChatModel model = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .build();

        assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .toolProvider(mcpClientService.createToolProvider())
                .build();
    }

    @PostConstruct
    public void init_Stream() {
        StreamingChatModel streamingModel = OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .build();

        assistant = AiServices.builder(Assistant.class)
                .streamingChatModel(streamingModel)
                .toolProvider(mcpClientService.createToolProvider())
                .build();

    }

    public TokenStream chat_Stream(String message) {
        // 延迟初始化，确保在第一次使用时创建
        if (assistant == null) {
            init_Stream();
        }

        try {
            return assistant.chat_Stream(message);
        } catch (Exception e) {
            // 可以在这里添加重试逻辑
            throw new RuntimeException("Chat service unavailable", e);
        }
    }

    public String chat(String message) {
        // 延迟初始化，确保在第一次使用时创建
        if (assistant == null) {
            init();
        }

        try {
            return assistant.chat(message);
        } catch (Exception e) {
            // 可以在这里添加重试逻辑
            throw new RuntimeException("Chat service unavailable", e);
        }
    }

    // 定义Assistant接口
    public interface Assistant {
        @dev.langchain4j.service.SystemMessage(
            """
                ## 角色
                你是一个智能旅游规划助手，能够为用户提供指定范围的旅游攻略。
                
                ## 任务
                若是用户需要某城市的旅游攻略，则进行如下流程：
                - 第一步，首先获取该城市近几天的天气预报，
                - 第二步，按照用户喜好，调用搜索引擎获取指定范围内的著名景点信息，一些热门景点最好有对应的附图链接。
                （注意，若是有给出附图，则一定仔细校验，避免出现图文不符的问题！）
                - 第三步，最后基于以上信息，分析给出用户所适合的旅游攻略。
                
                ## 输出
                - 第一，首先输出该城市未来几天的天气情况，给出出行建议（例如要带伞/防晒等），
                - 第二，然后将旅游攻略按天给出，例如第1天、第2天、等等。
            """)

        TokenStream chat_Stream(String userMessage);

        String chat(String userMessage);
    }
}