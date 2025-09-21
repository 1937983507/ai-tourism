package com.example.aitourism.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 请求：获取会话历史
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatHistoryRequest {
    private String sessionId;
}