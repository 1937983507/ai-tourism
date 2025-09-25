package com.example.aitourism.service;

import com.example.aitourism.dto.ChatMessageDTO;
import com.example.aitourism.dto.SessionListResponse;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

public interface ChatService {

    String chat(String sessionId, String messages, String userId, Boolean stream, HttpServletResponse response) throws Exception;

    List<ChatMessageDTO> getHistory(String sessionId);

    SessionListResponse getSessionList(Integer page, Integer pageSize, String userId);
}


