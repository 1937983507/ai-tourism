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
    user_id      varchar(64)                        not null comment '用户ID',
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




-- 用户表
create table if not exists t_user
(
    id            bigint auto_increment              primary key,
    user_id       varchar(64)                        not null comment '业务用户ID',
    phone         varchar(20)                        not null comment '手机号',
    password_hash varchar(200)                       not null comment 'BCrypt 密码',
    nickname      varchar(64)                        null,
    avatar        varchar(255)                       null,
    status        tinyint default 1                  not null comment '1=正常,0=禁用',
    created_time  datetime default CURRENT_TIMESTAMP not null,
    modify_time   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    constraint uk_user_phone unique (phone),
    constraint uk_user_userid unique (user_id)
) comment '用户表';

-- 角色表
create table if not exists t_role
(
    id          bigint auto_increment primary key,
    role_code   varchar(64) not null,
    role_name   varchar(64) not null,
    constraint uk_role_code unique (role_code)
) comment '角色表';

-- 用户角色关联
create table if not exists t_user_role
(
    id       bigint auto_increment primary key,
    user_id  varchar(64) not null,
    role_code varchar(64) not null,
    constraint fk_user_role_user foreign key (user_id) references t_user (user_id),
    constraint fk_user_role_role foreign key (role_code) references t_role (role_code)
) comment '用户角色关联表';

-- 刷新令牌表
create table if not exists t_refresh_token
(
    id                 bigint auto_increment primary key,
    user_id            varchar(64) not null,
    refresh_token      varchar(255) not null,
    expire_at          datetime not null,
    created_time       datetime default CURRENT_TIMESTAMP not null,
    constraint uk_refresh_token unique (refresh_token),
    index idx_user_id (user_id)
) comment '刷新令牌表';

-- 权限表
create table if not exists t_permission
(
    id           bigint auto_increment primary key,
    perm_code    varchar(128) not null comment '权限编码，如 ai:chat:read',
    perm_name    varchar(128) not null,
    constraint uk_perm_code unique (perm_code)
) comment '权限表';

-- 角色-权限 关联表
create table if not exists t_role_permission
(
    id         bigint auto_increment primary key,
    role_code  varchar(64)  not null,
    perm_code  varchar(128) not null,
    constraint fk_rp_role  foreign key (role_code) references t_role (role_code),
    constraint fk_rp_perm  foreign key (perm_code) references t_permission (perm_code)
) comment '角色权限关联表';



-- 角色
insert into t_role(role_code, role_name) values
('USER','普通用户'),
('ROOT','超级管理员')
on duplicate key update role_name=values(role_name);

-- 权限
insert into t_permission(perm_code, perm_name) values
('ai:session','会话列表'),
('ai:history','会话历史'),
('ai:chat','聊天发送与流式'),
('user:disable','禁用用户'),
('user:set-root','设为ROOT')
on duplicate key update perm_name=values(perm_name);

-- USER 角色权限（仅业务三项）
insert into t_role_permission(role_code, perm_code) values
('USER','ai:session'),
('USER','ai:history'),
('USER','ai:chat')
on duplicate key update perm_code=values(perm_code);

-- ROOT 角色权限（授予全部）
insert into t_role_permission(role_code, perm_code)
select 'ROOT', p.perm_code from t_permission p
on duplicate key update perm_code=values(perm_code);
