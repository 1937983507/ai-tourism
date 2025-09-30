package com.example.aitourism.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 会话列表响应
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SessionListResponse {
    private List<SessionDTO> sessionList;
    private Integer page;
    private Integer pageSize;
    private Integer total;
}