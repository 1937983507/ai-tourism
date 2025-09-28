package com.example.aitourism.service;

import com.example.aitourism.dto.ChatHistoryDTO;
import com.example.aitourism.dto.ChatHistoryResponse;
import com.example.aitourism.dto.SessionListResponse;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

public interface ChatService {

    String chat(String sessionId, String messages, String userId, Boolean stream, HttpServletResponse response) throws Exception;

    ChatHistoryResponse getHistory(String sessionId);

    SessionListResponse getSessionList(Integer page, Integer pageSize, String userId);
}


