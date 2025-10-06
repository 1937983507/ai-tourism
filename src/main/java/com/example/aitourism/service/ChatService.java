package com.example.aitourism.service;

import com.example.aitourism.dto.chat.ChatHistoryResponse;
import com.example.aitourism.dto.chat.SessionListResponse;
import reactor.core.publisher.Flux;

public interface ChatService {

    Flux<String> chat(String sessionId, String messages, String userId, Boolean stream) throws Exception;

    ChatHistoryResponse getHistory(String sessionId);

    SessionListResponse getSessionList(Integer page, Integer pageSize, String userId);
}


