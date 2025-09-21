package com.example.aitourism.mapper;

import com.example.aitourism.entity.ChatMessage;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ChatMessageMapper {

    @Insert("INSERT INTO t_ai_assistant_chat_messages(msg_id, session_id, user_name, role, content, title) " +
            "VALUES(#{msgId}, #{sessionId}, #{userName}, #{role}, #{content}, #{title})")
    int insert(ChatMessage message);

    @Select("SELECT * FROM t_ai_assistant_chat_messages WHERE session_id = #{sessionId} ORDER BY create_time ASC")
    List<ChatMessage> findBySessionId(String sessionId);
}
