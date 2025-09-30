package com.example.aitourism.ai;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.TokenStream;

// 定义Assistant接口
public interface AssistantService {
    @dev.langchain4j.service.SystemMessage(fromResource ="prompt/tour-route-planning-system-prompt.txt")
    TokenStream chat_Stream(@MemoryId String memoryId, @dev.langchain4j.service.UserMessage String userMessage);

    String chat(String userMessage);
}