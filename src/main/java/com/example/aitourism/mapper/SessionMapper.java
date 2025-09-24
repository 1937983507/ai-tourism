package com.example.aitourism.mapper;

import com.example.aitourism.entity.Session;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface SessionMapper {

    @Insert("INSERT INTO t_ai_assistant_sessions(session_id, user_name, title) " +
            "VALUES(#{sessionId}, #{userName}, #{title})")
    int insert(Session sessionId);

    @Select("SELECT * FROM t_ai_assistant_sessions WHERE session_id = #{sessionId}")
    Session findBySessionId(String sessionId);

    @Select("SELECT * FROM t_ai_assistant_sessions ORDER BY modify_time DESC LIMIT #{offset}, #{pageSize}")
    List<Session> findAll(@Param("offset") int offset, @Param("pageSize") int pageSize);

    @Select("SELECT COUNT(*) FROM t_ai_assistant_sessions")
    int count();

    @Update("UPDATE t_ai_assistant_sessions SET daily_routes = #{dailyRoutes} WHERE session_id = #{sessionId} ")
    void updateRoutine(@Param("dailyRoutes") String dailyRoutes, @Param("sessionId") String sessionId);

}