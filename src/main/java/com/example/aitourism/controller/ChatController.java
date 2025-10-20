package com.example.aitourism.controller;

import com.example.aitourism.dto.*;
import com.example.aitourism.dto.chat.*;
import com.example.aitourism.exception.InputValidationException;
import com.example.aitourism.service.ChatService;
import com.example.aitourism.service.impl.MemoryChatServiceImpl;
import com.example.aitourism.util.Constants;

import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/ai_assistant")
@Slf4j
public class ChatController {

    private final ChatService chatService;


    public ChatController(MemoryChatServiceImpl chatService) {
        this.chatService = chatService;
    }

    /**
     * 修改历史会话属性（0:置顶、1:取消置顶、2删除、3改标题）
     */
    @SaCheckLogin
    @SaCheckPermission("ai:session")
    @PostMapping("/session_modify")
    public BaseResponse<Void> sessionModify(@RequestBody SessionModifyRequest request) {
        if (request.getSessionId() == null) {
            return BaseResponse.error(Constants.ERROR_CODE_BAD_REQUEST, "缺少请求参数 session_id");
        }
        if (request.getOpType() == null) {
            return BaseResponse.error(Constants.ERROR_CODE_BAD_REQUEST, "缺少请求参数 op_type");
        }

        try {
            int op = request.getOpType();
            switch (op) {
                case 2: // 删除
                    boolean deleted = chatService.deleteSession(request.getSessionId());
                    if (!deleted) {
                        return BaseResponse.error(Constants.ERROR_CODE_BAD_REQUEST, "删除失败或会话不存在");
                    }
                    return BaseResponse.success();
                case 3: // 改标题
                    if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
                        return BaseResponse.error(Constants.ERROR_CODE_BAD_REQUEST, "缺少请求参数 title");
                    }
                    boolean renamed = chatService.renameSession(request.getSessionId(), request.getTitle().trim());
                    if (!renamed) {
                        return BaseResponse.error(Constants.ERROR_CODE_BAD_REQUEST, "修改标题失败或会话不存在");
                    }
                    return BaseResponse.success();
                default:
                    return BaseResponse.error(Constants.ERROR_CODE_BAD_REQUEST, "暂不支持的操作类型");
            }
        } catch (Exception e) {
            log.error("会话修改异常: {}", e.getMessage(), e);
            return BaseResponse.error(Constants.ERROR_CODE_SERVER_ERROR, "内部服务器出错: " + e.getMessage());
        }
    }


    /**
     * 发起流式对话（SSE）
     */
    @SaCheckLogin
    @SaCheckPermission("ai:chat")
    @PostMapping(value = "/chat-stream", produces = "text/event-stream")
    public Flux<String> chat_stream(@RequestBody ChatRequest request) {
        // 简单的参数校验（不可为空）
        if(request.getSessionId()==null){
            return Flux.just("data: {\"choices\":[{\"index\":0,\"text\":\"缺少session_id\",\"finish_reason\":\"stop\",\"model\":\"gpt-4o-mini\"}]}\n\n");
        }
        if(request.getMessages()==null){
            return Flux.just("data: {\"choices\":[{\"index\":0,\"text\":\"缺少messages\",\"finish_reason\":\"stop\",\"model\":\"gpt-4o-mini\"}]}\n\n");
        }
        if(request.getUserId()==null){
            return Flux.just("data: {\"choices\":[{\"index\":0,\"text\":\"缺少user_id\",\"finish_reason\":\"stop\",\"model\":\"gpt-4o-mini\"}]}\n\n");
        }
        try {
            return chatService.chat(request.getSessionId(), request.getMessages(), request.getUserId(), true);
        } catch (InputValidationException e) {
            String errEvent = String.format(
                "data: {\"choices\":[{\"index\":0,\"text\":\"%s\",\"finish_reason\":\"%s\",\"model\":\"%s\"}]}\n\n",
                "输入含不当内容，请修改后重试", "stop", "gpt-4o-mini"
            );
            return Flux.just(errEvent);
        } catch (Exception e) {
            log.error("聊天服务异常: {}", e.getMessage(), e);
            String errEvent = String.format(
                "data: {\"choices\":[{\"index\":0,\"text\":\"%s\",\"finish_reason\":\"%s\",\"model\":\"%s\"}]}\n\n",
                "内部服务器出错，请稍后重试", "stop", "gpt-4o-mini"
            );
            return Flux.just(errEvent);
        }
    }

    /**
     * 获取当前会话历史
     */
    @SaCheckLogin
    @SaCheckPermission("ai:history")
    @PostMapping("/get_history")
    public BaseResponse<ChatHistoryResponse> getHistory(@RequestBody ChatHistoryRequest request) {
        // 简单的参数校验
        if(request.getSessionId()==null){
            return BaseResponse.error(Constants.ERROR_CODE_BAD_REQUEST, "缺少请求参数 session_id");
        }

        try {
            // 调用业务层
            return BaseResponse.success(chatService.getHistory(request.getSessionId()));
        } catch (Exception e) {
            // 捕获所有其他异常，返回通用错误码和消息
            return BaseResponse.error(Constants.ERROR_CODE_SERVER_ERROR, "内部服务器出错: " + e.getMessage());
        }
    }

    /**
     * 获取所有历史会话
     */
    @SaCheckLogin
    @SaCheckPermission("ai:session")
    @PostMapping("/session_list")
    public BaseResponse<SessionListResponse> sessionList(@RequestBody SessionListRequest request) {
        // 简单的参数校验
        if(request.getPage()==null){
            return BaseResponse.error(Constants.ERROR_CODE_BAD_REQUEST, "缺少请求参数 page");
        }
        if(request.getPageSize()==null){
            return BaseResponse.error(Constants.ERROR_CODE_BAD_REQUEST, "缺少请求参数 page_size");
        }
        if(request.getUserId()==null){
            return BaseResponse.error(Constants.ERROR_CODE_BAD_REQUEST, "缺少请求参数 user_id");
        }        

        try {
            // 调用业务层
            return BaseResponse.success(chatService.getSessionList(request.getPage(), request.getPageSize(), request.getUserId()));
        } catch (Exception e) {
            // 捕获所有其他异常，返回通用错误码和消息
            return BaseResponse.error(Constants.ERROR_CODE_SERVER_ERROR, "内部服务器出错: " + e.getMessage());
        }
    }
}