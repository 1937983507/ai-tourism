# 数据库初始化

-- 创建库
create database if not exists aitourism;

-- 切换库
use aitourism;


-- 会话表
create table t_ai_assistant_sessions
(
    id           bigint auto_increment              primary key,
    session_id   varchar(64)                        not null comment '会话ID',
    user_name    varchar(64)                        not null comment '用户名',
    created_time datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    modify_time  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    title        varchar(255)                       not null comment '标题',
    daily_routes varchar(512)                       null,
    constraint session_id
        unique (session_id)
)comment 'AI 助手会话表';


-- 消息表
create table t_ai_assistant_chat_messages
(
    msg_id      varchar(64)                        not null comment '消息ID'          primary key,
    session_id  varchar(64)                        not null comment '会话ID',
    user_name   varchar(64)                        not null comment '用户名',
    role        varchar(32)                        not null comment '角色(user/assistant)',
    content     text                               not null comment '对话内容',
    title       varchar(255)                       null comment '标题',
    create_time datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    modify_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint fk_session
        foreign key (session_id) references t_ai_assistant_sessions (session_id)
)comment 'AI 助手消息表';

