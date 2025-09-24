package com.example.aitourism.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Session {
    private Long id;
    private String sessionId;
    private String userName;
    private LocalDateTime createdTime;
    private LocalDateTime modifyTime;
    private String title;
    private String dailyRoutes;
}