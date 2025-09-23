package com.example.aitourism.controller;

import com.example.aitourism.dto.*;
import com.example.aitourism.service.AssistantService;
import com.example.aitourism.service.ChatService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/ai_assistant")
public class ChatController {

    private final ChatService chatService;
    private final AssistantService assistantService;
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    public ChatController(ChatService chatService, AssistantService assistantService) {
        this.chatService = chatService;
        this.assistantService = assistantService;
    }

    /**
     * 发起对话
     */
    @PostMapping("/chat")
    public ApiResponse<String> chat(@RequestBody ChatRequest request) throws Exception {
        String reply = chatService.chat(request.getSessionId(), request.getMessages(), false, null);
        return ApiResponse.success(reply);
    }

    @PostMapping("/chat-stream")
    public ApiResponse<String> chat_stream(@RequestBody ChatRequest request, HttpServletResponse response) throws Exception {
        String reply = chatService.chat(request.getSessionId(), request.getMessages(), true, response);
        return ApiResponse.success(reply);
    }

    /**
     * 获取会话历史
     */
    @PostMapping("/get_history")
    public ApiResponse<List<ChatMessageDTO>> getHistory(@RequestBody ChatHistoryRequest request) {
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