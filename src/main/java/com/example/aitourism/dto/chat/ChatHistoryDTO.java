package com.example.aitourism.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单条聊天消息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatHistoryDTO {
    private String msgId;
    private String role;   // user 或 assistant
    private String content;
    private String modifyTime; // 消息修改时间（字符串）

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getModifyTime() {
        return modifyTime;
    }

    public void setModifyTime(String modifyTime) {
        this.modifyTime = modifyTime;
    }
}