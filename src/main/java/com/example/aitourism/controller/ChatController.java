package com.example.aitourism.controller;

import com.example.aitourism.dto.ApiResponse;
import com.example.aitourism.dto.ChatHistoryRequest;
import com.example.aitourism.dto.ChatMessageDTO;
import com.example.aitourism.dto.ChatRequest;
import com.example.aitourism.dto.SessionListRequest;
import com.example.aitourism.dto.SessionListResponse;
import com.example.aitourism.service.ChatService;
import com.example.aitourism.util.Constants;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;

import java.util.List;

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
            response.setStatus(Constants.ERROR_CODE_BAD_REQUEST);
            return;
        }
        if(request.getMessages()==null){
            response.setStatus(Constants.ERROR_CODE_BAD_REQUEST);
            return;
        }
        if(request.getUserId()==null){
            response.setStatus(Constants.ERROR_CODE_BAD_REQUEST);
            return;
        }

        try {
            // 调用业务层进行聊天逻辑
            chatService.chat(request.getSessionId(), request.getMessages(), request.getUserId(), true, response);
        } catch (Exception e) {
            // 捕获所有其他异常，返回通用错误码和消息
            response.setStatus(Constants.ERROR_CODE_SERVER_ERROR);
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
            return ApiResponse.error(Constants.ERROR_CODE_BAD_REQUEST, "缺少请求参数 session_id");
        }

        try {
            // 调用业务层
            return ApiResponse.success(chatService.getHistory(request.getSessionId()));
        } catch (Exception e) {
            // 捕获所有其他异常，返回通用错误码和消息
            return ApiResponse.error(Constants.ERROR_CODE_SERVER_ERROR, "内部服务器出错: " + e.getMessage());
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
            return ApiResponse.error(Constants.ERROR_CODE_BAD_REQUEST, "缺少请求参数 page");
        }
        if(request.getPageSize()==null){
            return ApiResponse.error(Constants.ERROR_CODE_BAD_REQUEST, "缺少请求参数 page_size");
        }
        if(request.getUserId()==null){
            return ApiResponse.error(Constants.ERROR_CODE_BAD_REQUEST, "缺少请求参数 user_id");
        }        

        try {
            // 调用业务层
            return ApiResponse.success(chatService.getSessionList(request.getPage(), request.getPageSize(), request.getUserId()));
        } catch (Exception e) {
            // 捕获所有其他异常，返回通用错误码和消息
            return ApiResponse.error(Constants.ERROR_CODE_SERVER_ERROR, "内部服务器出错: " + e.getMessage());
        }
    }
}