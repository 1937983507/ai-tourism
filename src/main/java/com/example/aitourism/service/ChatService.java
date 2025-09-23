package com.example.aitourism.service;

import com.example.aitourism.dto.*;
import com.example.aitourism.entity.ChatMessage;
import com.example.aitourism.entity.Session;
import com.example.aitourism.mapper.ChatMessageMapper;
import com.example.aitourism.mapper.SessionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ChatService {

    private final ChatMessageMapper chatMessageMapper;
    private final SessionMapper sessionMapper;

    private final AssistantService assistantService;

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

    public ChatService(ChatMessageMapper chatMessageMapper, SessionMapper sessionMapper, AssistantService assistantService) {
        this.chatMessageMapper = chatMessageMapper;
        this.sessionMapper = sessionMapper;
        this.assistantService = assistantService;
    }

    /**
     * 发起对话
     */
    public String chat(String sessionId, String messages) {
        logger.info("用户的问题是："+messages);

        // 如果 session 不存在，则创建
        Session session = sessionMapper.findBySessionId(sessionId);
        if (session == null) {
            session = new Session();
            session.setSessionId(sessionId);
            session.setUserName("default_user");
            session.setTitle(messages.length() > 10 ? messages.substring(0, 10) : messages);
            sessionMapper.insert(session);
        }

        // 保存用户消息
        ChatMessage userMsg = new ChatMessage();
        userMsg.setMsgId(UUID.randomUUID().toString());
        userMsg.setSessionId(sessionId);
        userMsg.setUserName("default_user");
        userMsg.setRole("user");
        userMsg.setTitle(session.getTitle());
        userMsg.setContent(messages);
        chatMessageMapper.insert(userMsg);

        // 模拟AI回复
//         String reply = "这是针对 [" + messages + "] 的AI回复";
        String reply = assistantService.chat(messages);


        ChatMessage assistantMsg = new ChatMessage();
        assistantMsg.setMsgId(UUID.randomUUID().toString());
        assistantMsg.setSessionId(sessionId);
        assistantMsg.setUserName("assistant");
        assistantMsg.setRole("assistant");
        assistantMsg.setTitle(session.getTitle());
        assistantMsg.setContent(reply);
        chatMessageMapper.insert(assistantMsg);

        return reply;
    }

    /**
     * 获取会话历史
     */
    public List<ChatMessageDTO> getHistory(String sessionId) {
        List<ChatMessage> messages = chatMessageMapper.findBySessionId(sessionId);
        List<ChatMessageDTO> result = new ArrayList<>();
        for (ChatMessage m : messages) {
            ChatMessageDTO dto = new ChatMessageDTO(m.getMsgId(), m.getRole(), m.getContent());
            result.add(dto);
        }
        return result;
    }

    /**
     * 获取会话列表
     */
    public SessionListResponse getSessionList(Integer page, Integer pageSize) {
        int offset = (page - 1) * pageSize;
        List<Session> list = sessionMapper.findAll(offset, pageSize);
        int total = sessionMapper.count();

        List<SessionDTO> dtoList = new ArrayList<>();
        for (Session s : list) {
            SessionDTO dto = new SessionDTO(s.getSessionId(), s.getModifyTime().toString(), s.getTitle());
            dtoList.add(dto);
        }

        SessionListResponse resp = new SessionListResponse();
        resp.setSessionList(dtoList);
        resp.setPage(page);
        resp.setPageSize(pageSize);
        resp.setTotal(total);
        return resp;
    }
}