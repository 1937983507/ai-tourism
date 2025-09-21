package com.example.aitourism.controller;

import com.example.aitourism.dto.*;
import com.example.aitourism.service.ChatService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ai_assistant")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * 发起对话
     */
    @PostMapping("/chat")
    public ApiResponse<String> chat(@RequestBody ChatRequest request) {
        String reply = chatService.chat(request.getSessionId(), request.getMessages());
        return ApiResponse.success(reply);
    }

    /**
     * 获取会话历史
     */
    @PostMapping("/get_history")
    public ApiResponse<List<ChatMessageDTO>> getHistory(@RequestBody ChatHistoryRequest request) {
        System.out.println(request);
        return ApiResponse.success(chatService.getHistory(request.getSessionId()));
    }

    /**
     * 获取所有历史会话
     */
    @PostMapping("/session_list")
    public ApiResponse<SessionListResponse> sessionList(@RequestBody SessionListRequest request) {
        return ApiResponse.success(chatService.getSessionList(request.getPage(), request.getPageSize()));
    }
}