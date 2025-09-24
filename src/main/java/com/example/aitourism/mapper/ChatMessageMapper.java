package com.example.aitourism.mapper;

import com.example.aitourism.entity.Message;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ChatMessageMapper {

    @Insert("INSERT INTO t_ai_assistant_chat_messages(msg_id, session_id, user_name, role, content, title) " +
            "VALUES(#{msgId}, #{sessionId}, #{userName}, #{role}, #{content}, #{title})")
    int insert(Message message);

    @Select("SELECT * FROM t_ai_assistant_chat_messages WHERE session_id = #{sessionId} ORDER BY create_time ASC")
    List<Message> findBySessionId(String sessionId);

    // 新增删除方法
    @Delete("DELETE FROM t_ai_assistant_chat_messages WHERE session_id = #{sessionId}")
    int deleteBySessionId(String sessionId);

    // 可选：查询消息数量，用于记忆窗口控制
    @Select("SELECT COUNT(*) FROM t_ai_assistant_chat_messages WHERE session_id = #{sessionId}")
    int countBySessionId(String sessionId);

}
