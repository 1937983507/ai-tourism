package com.example.aitourism.dto.chat;

import lombok.Data;

@Data
public class SessionModifyRequest {
    private String sessionId;
    private Integer opType; // 0=置顶，1=取消置顶，2=删除, 3=修改标题
    private String title;   // 当 opType=3 时可传
    private String userId;
}


