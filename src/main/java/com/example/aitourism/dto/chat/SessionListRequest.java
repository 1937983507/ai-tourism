package com.example.aitourism.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 请求：获取所有历史会话
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SessionListRequest {
    private Integer page;
    private Integer pageSize;
    private String userId;
}