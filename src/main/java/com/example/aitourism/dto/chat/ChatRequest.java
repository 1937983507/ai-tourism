package com.example.aitourism.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 请求：发起对话
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatRequest {
    private String sessionId;
    private String messages;
    private String userId;
}
