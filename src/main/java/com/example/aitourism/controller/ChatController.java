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
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * 发起对话
     */
    @PostMapping("/chat")
    public ApiResponse<String> chat(@RequestBody ChatRequest request) throws Exception {
        // 简单的参数校验
        if(request.getSessionId()==null){
            System.out.println("400");
            return ApiResponse.error(400, "缺少请求参数 session_id");
        }
        if(request.getMessages()==null){
            System.out.println("400");
            return ApiResponse.error(400, "缺少请求参数 messages");
        }

        try {
            // 调用业务层进行聊天逻辑
            String reply = chatService.chat(request.getSessionId(), request.getMessages(), false, null);
            return ApiResponse.success(reply);
        } catch (Exception e) {
            // 捕获所有其他异常，返回通用错误码和消息
            return ApiResponse.error(500, "内部服务器出错: " + e.getMessage());
        }
    }

    /**
     * 发起流式对话
     */
    @PostMapping("/chat-stream")
    public ApiResponse<String> chat_stream(@RequestBody ChatRequest request, HttpServletResponse response) throws Exception {
        // 简单的参数校验
        if(request.getSessionId()==null){
            System.out.println("400");
            return ApiResponse.error(400, "缺少请求参数 session_id");
        }
        if(request.getMessages()==null){
            System.out.println("400");
            return ApiResponse.error(400, "缺少请求参数 messages");
        }

        try {
            // 调用业务层进行聊天逻辑
            String reply = chatService.chat(request.getSessionId(), request.getMessages(), true, response);
            return ApiResponse.success(reply);
        } catch (Exception e) {
            // 捕获所有其他异常，返回通用错误码和消息
            return ApiResponse.error(500, "内部服务器出错: " + e.getMessage());
        }
    }

    /**
     * 获取会话历史
     */
    @PostMapping("/get_history")
    public ApiResponse<List<ChatMessageDTO>> getHistory(@RequestBody ChatHistoryRequest request) {
        // 简单的参数校验
        if(request.getSessionId()==null){
            System.out.println("400");
            return ApiResponse.error(400, "缺少请求参数 session_id");
        }

        try {
            // 调用业务层
            return ApiResponse.success(chatService.getHistory(request.getSessionId()));
        } catch (Exception e) {
            // 捕获所有其他异常，返回通用错误码和消息
            return ApiResponse.error(500, "内部服务器出错: " + e.getMessage());
        }
    }

    /**
     * 获取所有历史会话
     */
    @PostMapping("/session_list")
    public ApiResponse<SessionListResponse> sessionList(@RequestBody SessionListRequest request) {
        // 简单的参数校验
        if(request.getPage()==null){
            System.out.println("400");
            return ApiResponse.error(400, "缺少请求参数 page");
        }
        if(request.getPageSize()==null){
            System.out.println("400");
            return ApiResponse.error(400, "缺少请求参数 page_size");
        }

        try {
            // 调用业务层
            return ApiResponse.success(chatService.getSessionList(request.getPage(), request.getPageSize()));
        } catch (Exception e) {
            // 捕获所有其他异常，返回通用错误码和消息
            return ApiResponse.error(500, "内部服务器出错: " + e.getMessage());
        }
    }
}