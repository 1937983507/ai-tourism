package com.example.aitourism.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessage {
    private String msgId;
    private String sessionId;
    private String userName;
    private String role;
    private String content;
    private String title;
    private LocalDateTime createTime;
    private LocalDateTime modifyTime;
}