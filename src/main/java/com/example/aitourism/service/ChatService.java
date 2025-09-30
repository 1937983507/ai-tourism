package com.example.aitourism.service;

import com.example.aitourism.dto.chat.ChatHistoryResponse;
import com.example.aitourism.dto.chat.SessionListResponse;
import jakarta.servlet.http.HttpServletResponse;

public interface ChatService {

    String chat(String sessionId, String messages, String userId, Boolean stream, HttpServletResponse response) throws Exception;

    ChatHistoryResponse getHistory(String sessionId);

    SessionListResponse getSessionList(Integer page, Integer pageSize, String userId);
}


