package com.example.aitourism.service;

import ch.qos.logback.classic.Logger;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import org.springframework.web.context.WebApplicationContext;

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
                .chatLanguageModel(model)
                .toolProvider(mcpClientService.createToolProvider())
                .build();
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
                你是一个旅游规划助手，能够为用户提供某座城市的旅游攻略。
                
                ## 任务
                若是用户需要某城市的旅游攻略，则进行如下流程：
                首先获取该城市近几天的天气预报，结合调用搜索引擎获取该城市著名的景点。
                最后于此，分析其所适合的旅游攻略。
                
                ## 输出
                首先输出该城市未来几天的天气情况，给出出行建议（例如带伞/注意防晒等），
                然后将旅游攻略按天给出，例如第1天、第2天、等等。
            """)
        String chat(String userMessage);
    }
}