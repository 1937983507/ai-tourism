package com.example.aitourism.controller;

import com.example.aitourism.dto.*;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.example.aitourism.service.ChatService;
import com.example.aitourism.service.impl.AssistantService;

import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import cn.dev33.satoken.stp.StpUtil;

@RestController
@RequestMapping("/ai_assistant")
public class ChatController {

    private final ChatService chatService;


    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }


    /**
     * 发起流式对话（SSE）
     */
    @SaCheckLogin
    @SaCheckPermission("ai:chat")
    @PostMapping(value = "/chat-stream", produces = "text/event-stream")
    public void chat_stream(@RequestBody ChatRequest request, HttpServletResponse response) throws Exception {
        // 简单的参数校验
        if(request.getSessionId()==null){
            System.out.println("400");
            response.setStatus(400);
            return;
        }
        if(request.getMessages()==null){
            System.out.println("400");
            response.setStatus(400);
            return;
        }
        if(request.getUserId()==null){
            System.out.println("400");
            response.setStatus(400);
            return;
        }

        try {
            // 调用业务层进行聊天逻辑
            chatService.chat(request.getSessionId(), request.getMessages(), request.getUserId(), true, response);
        } catch (Exception e) {
            // 捕获所有其他异常，返回通用错误码和消息
            response.setStatus(500);
            try {
                response.getWriter().write("data: {\"error\":\"内部服务器出错\"}\n\n");
                response.getWriter().flush();
            } catch (Exception ignored) {}
        }
    }

    /**
     * 获取会话历史
     */
    @SaCheckLogin
    @SaCheckPermission("ai:history")
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
    @SaCheckLogin
    @SaCheckPermission("ai:session")
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
        if(request.getUserId()==null){
            System.out.println("400");
            return ApiResponse.error(400, "缺少请求参数 user_id");
        }        

        try {
            // 调用业务层
            return ApiResponse.success(chatService.getSessionList(request.getPage(), request.getPageSize(), request.getUserId()));
        } catch (Exception e) {
            // 捕获所有其他异常，返回通用错误码和消息
            return ApiResponse.error(500, "内部服务器出错: " + e.getMessage());
        }
    }
}