package com.example.aitourism.entity;

import lombok.Data;

@Data
public class User {
    private Long id;
    private String userId;
    private String phone;
    private String passwordHash;
    private String nickname;
    private String avatar;
    private Integer status;
}


