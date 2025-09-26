package com.example.aitourism.mapper;

import com.example.aitourism.entity.User;
import org.apache.ibatis.annotations.*;

@Mapper
public interface UserMapper {

    @Insert("INSERT INTO t_user(user_id, phone, password_hash, nickname, avatar, status) " +
            "VALUES(#{userId}, #{phone}, #{passwordHash}, #{nickname}, #{avatar}, #{status})")
    int insert(User user);

    @Select("SELECT * FROM t_user WHERE phone = #{phone}")
    User findByPhone(String phone);

    @Select("SELECT * FROM t_user WHERE user_id = #{userId}")
    User findByUserId(String userId);

    @Update("UPDATE t_user SET status = #{status} WHERE user_id = #{userId}")
    int updateStatusByUserId(@Param("userId") String userId, @Param("status") int status);
}


