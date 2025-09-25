package com.example.aitourism.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RefreshToken {
    private Long id;
    private String userId;
    private String refreshToken;
    private LocalDateTime expireAt;
}


