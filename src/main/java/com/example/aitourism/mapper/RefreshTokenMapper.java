package com.example.aitourism.mapper;

import com.example.aitourism.entity.RefreshToken;
import org.apache.ibatis.annotations.*;

@Mapper
public interface RefreshTokenMapper {

    @Insert("INSERT INTO t_refresh_token(user_id, refresh_token, expire_at) VALUES(#{userId}, #{refreshToken}, #{expireAt})")
    int insert(RefreshToken token);

    @Select("SELECT * FROM t_refresh_token WHERE refresh_token = #{token}")
    RefreshToken findByToken(String token);

    @Delete("DELETE FROM t_refresh_token WHERE user_id = #{userId}")
    int deleteByUserId(String userId);
}


