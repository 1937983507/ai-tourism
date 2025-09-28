# AI 智能旅游规划助手 API 文档

> 本文档详细说明后端各主要 RESTful API 的请求方式、参数、返回格式、权限要求及错误码。

---

## 目录

- [AI 助手相关接口](#ai-助手相关接口)
- [会话与历史管理接口](#会话与历史管理接口)
- [用户与认证相关接口](#用户与认证相关接口)
- [通用返回结构](#通用返回结构)
- [错误码说明](#错误码说明)

---

## AI 助手相关接口

### 1. 发起 AI 流式对话
- **接口**：`POST /ai_assistant/chat`
- **描述**：发起 AI 对话，支持 SSE 流式返回，返回结构化旅游路线与建议
- **请求头**：`Authorization: Bearer <token>`
- **请求参数**（JSON）：
  ```json
  {
    "session_id": "string (会话ID)",
    "messages": "string (用户输入内容)",
    "user_id": "string (当前用户ID)"
  }
  ```
- **返回**：SSE 流式响应，内容为 Markdown 格式的 AI 回复，附带结构化路线数据（JSON）。
- **返回示例**:
  ```json

    data: {"choices":[{"index":0,"text":"请","finish_reason":"stop","model":"gpt-4o-mini"}]}

    data: {"choices":[{"index":0,"text":"稍","finish_reason":"stop","model":"gpt-4o-mini"}]}

    data: {"choices":[{"index":0,"text":"等","finish_reason":"stop","model":"gpt-4o-mini"}]}

    data: {"choices":[{"index":0,"text":"，我","finish_reason":"stop","model":"gpt-4o-mini"}]}

    data: {"choices":[{"index":0,"text":"将","finish_reason":"stop","model":"gpt-4o-mini"}]}

    data: {"choices":[{"index":0,"text":"为","finish_reason":"stop","model":"gpt-4o-mini"}]}

    data: {"choices":[{"index":0,"text":"您","finish_reason":"stop","model":"gpt-4o-mini"}]}

    ...

    data: {"choices":[{"finish_reason":"stop"}]}

  ```
- **权限**：`ai:chat` 权限

---

## 会话与历史管理接口

### 1. 获取会话历史
- **接口**：`POST /ai_assistant/get_history`
- **描述**：获取指定会话的历史消息
- **请求头**：`Authorization: Bearer <token>`
- **请求参数**（JSON）：
  ```json
  {
    "session_id": "string"
  }
  ```
- **返回示例**：
  ```json
  {
    "code": 0,
    "msg": "success",
    "data": [
      {
        "msg_id": "string",
        "role": "user|assistant",
        "content": "string"
      }
    ]
  }
  ```
- **权限**：`ai:history` 权限

---

### 2. 获取历史会话列表
- **接口**：`POST /ai_assistant/session_list`
- **描述**：分页获取当前用户的历史会话
- **请求头**：`Authorization: Bearer <token>`
- **请求参数**（JSON）：
  ```json
  {
    "user_id": "string",
    "page": 1,
    "page_size": 10
  }
  ```
- **返回示例**：
  ```json
  {
    "code": 0,
    "msg": "success",
    "data": {
      "session_list": [
        {
          "session_id": "string",
          "modify_time": "2024-06-01T12:00:00",
          "title": "string",
          "daily_routes": "string (结构化路线JSON)"
        }
      ],
      "page": 1,
      "page_size": 10,
      "total": 100
    }
  }
  ```
- **权限**：`ai:session` 权限

---


## 用户与认证相关接口

### 1. 用户注册
- **接口**：`POST /auth/register`
- **描述**：新用户注册，自动分配 USER 角色
- **请求参数**（JSON）：
  ```json
  {
    "phone": "string (手机号)",
    "password": "string (明文密码)",
    "nickname": "string (昵称，可选)"
  }
  ```
- **返回示例**：
  ```json
  {
    "code": 0,
    "msg": "success",
    "data": {
      "user_id": "string"
    }
  }
  ```
- **错误码**：2001（手机号已存在）
- **权限**：无需登录

---

### 2. 用户登录
- **接口**：`POST /auth/login`
- **描述**：用户登录，返回 token、用户信息、刷新令牌等
- **请求参数**（JSON）：
  ```json
  {
    "phone": "string",
    "password": "string"
  }
  ```
- **返回示例**：
  ```json
  {
    "code": 0,
    "msg": "success",
    "data": {
      "token": "string",
      "expires_in": 7200,
      "refresh_token": "string",
      "refresh_expires_in": 2592000,
      "user": {
        "user_id": "string",
        "nickname": "string",
        "avatar": "string"
      }
    }
  }
  ```
- **错误码**：1001（账号或密码错误）
- **权限**：无需登录

---

### 3. 获取当前用户信息
- **接口**：`GET /auth/me`
- **描述**：获取当前登录用户信息及角色
- **请求头**：`Authorization: Bearer <token>`
- **返回示例**：
  ```json
  {
    "code": 0,
    "msg": "success",
    "data": {
      "user_id": "string",
      "phone": "string",
      "nickname": "string",
      "avatar": "string",
      "roles": ["USER"]
    }
  }
  ```
- **权限**：需登录

---

### 4. 刷新 Token
- **接口**：`POST /auth/refresh`
- **描述**：使用 refresh_token 换取新的访问 token
- **请求参数**（JSON）：
  ```json
  {
    "refresh_token": "string"
  }
  ```
- **返回示例**：
  ```json
  {
    "code": 0,
    "msg": "success",
    "data": {
      "token": "string",
      "expires_in": 7200
    }
  }
  ```
- **错误码**：1101（token 失效）
- **权限**：无需登录

---

### 5. 用户登出
- **接口**：`POST /auth/logout`
- **描述**：登出当前用户
- **请求头**：`Authorization: Bearer <token>`
- **返回示例**：
  ```json
  {
    "code": 0,
    "msg": "success"
  }
  ```
- **权限**：需登录

---

### 6. 禁用用户
- **接口**：`POST /auth/disable`
- **描述**：禁用指定用户（需权限）
- **请求头**：`Authorization: Bearer <token>`
- **请求参数**（JSON）：
  ```json
  {
    "user_id": "string"
  }
  ```
- **返回示例**：同上
- **权限**：`user:disable` 权限

---

### 7. 设为 ROOT
- **接口**：`POST /auth/set_root`
- **描述**：授予用户 ROOT 角色（需权限）
- **请求头**：`Authorization: Bearer <token>`
- **请求参数**（JSON）：
  ```json
  {
    "user_id": "string"
  }
  ```
- **返回示例**：同上
- **权限**：`user:set-root` 权限

---

## 通用返回结构

所有接口均返回如下结构：
```json
{
  "code": 0,
  "msg": "success",
  "data": ...
}
```
- `code`：0 表示成功，非0为错误码
- `msg`：消息文本
- `data`：返回数据体

---

## 错误码说明

| 错误码 | 说明 |
| ------ | ---- |
| 0      | 成功 |
| 1001   | 账号或密码错误 |
| 1101   | 未认证或 token 失效 |
| 2001   | 注册手机号已存在 |
| 4000   | 参数错误 |
| 5000   | 服务端错误 |

如遇其它错误，`msg` 字段会返回详细说明。

---

## 权限说明

- `ai:chat`：AI 聊天权限
- `ai:history`：会话历史权限
- `ai:session`：会话列表权限
- `user:disable`：禁用用户权限
- `user:set-root`：ROOT 授权权限