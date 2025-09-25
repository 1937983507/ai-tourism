package com.example.aitourism.service.impl;


import com.example.aitourism.entity.Message;
import com.example.aitourism.entity.Session;
import com.example.aitourism.mapper.ChatMessageMapper;
import com.example.aitourism.mapper.SessionMapper;
import dev.langchain4j.data.message.*;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

// 用于手动管理记忆
@Service
@Slf4j
public class ChatMemoryStoreService implements ChatMemoryStore {

//        private final DB db = DBMaker.fileDB("multi-user-chat-memory.db").transactionEnable().make();
//        private final Map<Integer, String> map = db.hashMap("messages", INTEGER, STRING).createOrOpen();

    private final SessionMapper sessionMapper;
    private final ChatMessageMapper chatMessageMapper;

    public ChatMemoryStoreService(SessionMapper sessionMapper, ChatMessageMapper chatMessageMapper) {
        this.sessionMapper = sessionMapper;
        this.chatMessageMapper = chatMessageMapper;
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        // 从数据库按sessionId查询所有消息，并转换为LangChain4j的ChatMessage
        List<Message> dbMessages = chatMessageMapper.findBySessionId(memoryId.toString());
        return convertToL4jMessages(dbMessages);
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        // 根据memoryId将消息列表更新到数据库
//            String json = messagesToJson(messages);
//            map.put((int) memoryId, json);
//            db.commit();


        // 先删除该session的所有旧消息
        // 注意：这里需要您补充删除方法到Mapper中
        // chatMessageMapper.deleteBySessionId(sessionId);

//        // 将LangChain4j的ChatMessage转换为数据库实体并保存
//        for (int i = 0; i < messages.size(); i++) {
//            ChatMessage l4jMessage = messages.get(i);
//            Message dbMessage = convertToDbMessage(memoryId.toString(), l4jMessage, i);
//            chatMessageMapper.insert(dbMessage);
//        }

        // 获取当前数据库中已有的消息数量
        int existingCount = chatMessageMapper.countBySessionId(memoryId.toString());

        // 只保存新消息（从existingCount开始）
        for (int i = existingCount; i < messages.size(); i++) {
            ChatMessage l4jMessage = messages.get(i);
            Message dbMessage = convertToDbMessage(memoryId.toString(), l4jMessage, i);
            chatMessageMapper.insert(dbMessage);
        }

    }


    @Override
    public void deleteMessages(Object memoryId) {
        // 根据memoryId删除消息
//            map.remove((int) memoryId);
//            db.commit();
    }

    private List<ChatMessage> convertToL4jMessages(List<Message> dbMessages) {
        return dbMessages.stream().map(dbMessage -> {
            switch (dbMessage.getRole().toLowerCase()) {
                case "user":
                    return new UserMessage(dbMessage.getContent());
                case "assistant":
                    return new AiMessage(dbMessage.getContent());
                case "system":
                    return new SystemMessage(dbMessage.getContent());
                default:
                    return new UserMessage(dbMessage.getContent());
            }
        }).collect(Collectors.toList());
    }

    private Message convertToDbMessage(String sessionId, ChatMessage l4jMessage, int index) {
        Session session = sessionMapper.findBySessionId(sessionId);

        Message dbMessage = new Message();
        dbMessage.setMsgId(generateMsgId(sessionId, index));
        dbMessage.setSessionId(sessionId);
        dbMessage.setRole(getRoleFromMessage(l4jMessage));
        dbMessage.setContent(getContentFromMessage(l4jMessage));
        dbMessage.setTitle(session.getTitle()); // 可根据内容生成更有意义的标题
        return dbMessage;
    }

    private String getContentFromMessage(ChatMessage message) {
        // 方法1：使用类型检查
        if (message instanceof UserMessage) {
            return ((UserMessage) message).singleText();
        } else if (message instanceof AiMessage) {
            return ((AiMessage) message).text();
        } else if (message instanceof SystemMessage) {
            return ((SystemMessage) message).text();
        } else if (message instanceof ToolExecutionResultMessage) {
            return ((ToolExecutionResultMessage) message).text();
        }

        // 方法2：使用toString()作为备选（可能包含额外信息）
        return message.toString();
    }

    private String getRoleFromMessage(ChatMessage message) {
        if (message instanceof UserMessage) return "user";
        if (message instanceof AiMessage) return "assistant";
        if (message instanceof SystemMessage) return "system";
        return "user";
    }

    private String generateMsgId(String sessionId, int index) {
        return UUID.randomUUID().toString();
//            return sessionId + "_" + System.currentTimeMillis() + "_" + index;
    }
}