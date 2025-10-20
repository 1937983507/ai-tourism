package com.example.aitourism.service;

import com.example.aitourism.dto.chat.ChatHistoryResponse;
import com.example.aitourism.dto.chat.SessionListResponse;
import reactor.core.publisher.Flux;

public interface ChatService {

    // 发起对话请求
    Flux<String> chat(String sessionId, String messages, String userId, Boolean stream) throws Exception;

    // 获取当前会话的对话历史
    ChatHistoryResponse getHistory(String sessionId);

    // 获取会话列表
    SessionListResponse getSessionList(Integer page, Integer pageSize, String userId);

    // 删除会话（及其消息）
    boolean deleteSession(String sessionId);

    // 修改会话标题
    boolean renameSession(String sessionId, String newTitle);
}


