package com.example.aitourism.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 当前会话消息响应
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatHistoryResponse {
    private List<ChatHistoryDTO> historyList;
    private Integer total;
}