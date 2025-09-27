# AI 智能旅游规划助手（后端）

> 访问地址：[http://your-server-address.com](http://your-server-address.com)  
> 欢迎体验智能旅游规划服务！

## 📖 项目简介

aI-tourism 是一个智能旅游规划系统，后端基于 Spring Boot、LangChain4j、MySQL、MyBatis、Sa-Token 等技术栈，集成了多种 AI 能力（如 AI Service、MCP 工具等），为用户提供个性化旅游路线推荐、会话管理、用户权限管理等功能。系统支持多轮对话、地图路线可视化、权限安全、MCP 工具热插拔等特性。

---

## 📚 目录

- [📖 项目简介](#项目简介)
- [💡 核心特性与架构特点](#核心特性与架构特点)
- [🏗️ 系统整体架构](#系统整体架构)
- [🚀 快速开始](#快速开始)
  - [📂 目录结构](#目录结构)
  - [🛠️ 技术栈与依赖](#技术栈与依赖)
  - [🗄️ 数据库结构](#数据库结构)
  - [⚙️ 配置说明](#配置说明)
  - [🔗 接口说明](#接口说明)
- [🔒 认证与权限机制](#认证与权限机制)
- [🤖 AI 能力集成与多轮对话](#ai-能力集成与多轮对话)
- [🛫 部署与运行](#部署与运行)
- [🖼️ 示例效果](#示例效果)
- [📬 联系与贡献](#联系与贡献)
- [📝 License](#license)

---

## 💡 核心特性与架构特点

### 1. Agent 服务与地图路线渲染深度结合
- **功能说明**：用户不仅能获得 Agent 生成的图文旅游攻略，还能在前端地图上直观查看每日路线规划。
- **实现方式**：
    - 后端 chat 接口调用 Agent 服务，自动执行 MCP 工具获取天气信息与景点信息，SSE 流式返回给前端。除此以外，还调用小模型，通过 Prompt few-shot 结合 JSON Scheam 输出结构化的路线数据（含每日途径点）。
    - 前端在接收流式响应后，通过 marked.js 和 Highlight.js 等库，实现 Markdown 内容的渲染与语法高亮；另外解析结构化路线数据，结合地图组件（高德 JS API）进行可视化渲染。
- **优势**：极大提升用户体验，实现“所见即所得”的智能路线规划。

### 2. 基于 Langchain4j 的 Agent 服务，支持 MCP 工具热插拔，多轮对话记忆
- **功能说明**：AI Agent 通过 Langchain4j 框架集成，支持多种MCP工具（如天气、景点搜索等）动态加载，具备短期对话记忆与长期数据库存储。
- **实现方式**:
    - MCP 工具通过配置可灵活启用/禁用，支持热插拔。
    - 对话短期记忆通过内存管理，长期记忆(历史会话、消息)持久化到数据库。
    - 多轮对话上下文由 sessionId 关联，支持历史追溯与上下文理解。
- **优势**：灵活扩展 AI 能力，支持复杂多轮对话，兼顾性能与可维护性。

### 3. token 超限防护：JDK 动态代理与 ToolProvider 包装器
- **功能说明**：防止 LLM 调用时因输出内容过长导致 token 超限报错。
- **实现方式**:
    - 采用 JDK 动态代理拦截 Agent 输入内容，自动裁剪超长文本。
    - MCP 工具执行结果通过 ToolProvider 包装器进行内容截断，确保返回内容不超限。
    - 智能摘要与结构化输出，最大化信息密度。
- **优势**：保障系统稳定性，提升 AI 响应质量，避免因 token 限制导致的服务中断。

### 4. Sa-Token 权限认证：短期+长期 Token 结合
- **功能说明**：实现高效且安全的用户认证与权限管理。
- **实现方式**:
    - 短期 Token(JwT) 存储于内存，响应速度快，适合高效访问。
    - 长期 Token(Refresh Token) 持久化于数据库，支持自动续签与安全退出。
    - 结合注解式权限控制，细粒度分配角色与权限。
- **优势**:兼顾性能与安全，支持多端登录、权限动态调整。

### 5. SpringBoot 工程化与 RESTful 设计
- **功能说明**：后端采用标准 SpringBoot 工程化架构，接口遵循 RESTful 规范，便于前后端分离与扩展。
- **实现方式**:
    - 统一的 Controller - Service - Mapper 分层，清晰职责划分。
    - 接口风格统一，支持 JSON 数据交互，便于前端集成。
- **优势**：易于维护、扩展和团队协作，支持微服务化演进。

---

## 🏗️ 系统整体架构

> ![请在此处补充系统整体架构图](system-architecture.png)

本系统采用分层解耦、模块化设计，主要业务模块如下：

### 1. 用户与权限模块
- 负责用户注册、登录、登出、信息查询、角色与权限分配、token 管理等。
- 基于 Sa-Token 实现 JWT 认证、权限注解、短期/长期 token 结合，保障安全与效率。

### 2. AI Agent 智能助手模块
- 基于 LangChain4j 实现，支持多轮对话、上下文记忆。
- 动态集成 MCP 工具（如天气、景点搜索等），支持热插拔。
- 具备 token 超限防护（JDK 动态代理+ToolProvider 裁剪），输出结构化旅游路线。

### 3. 会话与记忆管理模块
- 支持多轮对话的短期记忆（内存）与长期记忆（数据库持久化）。
- 会话历史、消息、路线等数据结构化存储，便于追溯与分析。

### 4. 地图与可视化模块
- 后端输出结构化路线数据，前端解析并结合地图组件（如高德/百度地图）渲染每日旅游路线。
- 实现“所见即所得”的智能路线规划体验。

### 5. 工具与集成模块
- MCP 工具统一注册与管理，支持按需加载、灵活扩展。
- 支持多种大模型（OpenAI、HuggingFace、VertexAI 等）能力接入。

### 6. 数据与安全模块
- MySQL 持久化存储用户、角色、权限、会话、消息、token 等业务数据。
- 全局异常处理、日志、CORS 配置，保障系统稳定与安全。

---

## 🚀 快速开始

### 📂 目录结构

```
ai-tourism/
├── src/
│   ├── main/
│   │   ├── java/com/example/aitourism/
│   │   │   ├── controller/      # REST API 控制器
│   │   │   ├── service/         # 业务逻辑与AI集成
│   │   │   ├── entity/          # 实体类
│   │   │   ├── mapper/          # MyBatis 映射
│   │   │   ├── dto/             # 数据传输对象
│   │   │   ├── config/          # 配置类（如Sa-Token、CORS等）
│   │   │   └── util/            # 工具类
│   │   └── resources/
│   │       ├── application.yml  # 主要配置文件
│   │       └── sql/             # 数据库建表脚本
├── pom.xml                      # Maven 依赖
└── README.md
```

### 🛠️ 技术栈与依赖

- Java 21
- Spring Boot 3.5.x
- LangChain4j（AI能力集成，支持 OpenAI、MCP、HuggingFace 等）
- MySQL 8.x
- MyBatis & MyBatis-Spring-Boot
- Sa-Token（JWT 认证与权限）
- BCrypt（密码加密）
- Lombok
- OkHttp3

> 详见 `pom.xml` 依赖配置。

### 🗄️ 数据库结构

主要表设计如下（详见 `sql/create_table.sql`）：

- `t_user`：用户表（含手机号、加密密码、昵称、头像、状态等）
- `t_role`：角色表（如 USER、ROOT）
- `t_permission`：权限表
- `t_user_role`：用户-角色关联表
- `t_role_permission`：角色-权限关联表
- `t_refresh_token`：刷新令牌表
- `t_ai_assistant_sessions`：AI助手会话表
- `t_ai_assistant_chat_messages`：AI助手消息表

> 详细字段和约束请参考 `sql/create_table.sql`。

### ⚙️ 配置说明

主要配置项在 `src/main/resources/application.yml`：

- 端口、数据库连接、日志、MyBatis、Sa-Token、OpenAI/MCP 等 AI 服务参数
- Sa-Token JWT 密钥、token 过期时间、权限注解等
- MCP 工具裁剪、AI模型参数等

### 🔗 接口说明

#### 用户与认证相关
- `POST /auth/login`：用户登录，返回 token、用户信息等
- `POST /auth/register`：用户注册，自动分配 USER 角色
- `GET /auth/me`：获取当前用户信息及角色
- `POST /auth/refresh`：刷新 token，提升安全性与体验
- `POST /auth/logout`：登出，清理会话
- `POST /auth/disable`：禁用用户（需权限）
- `POST /auth/set_root`：ROOT 授权（需权限）

#### AI 助手相关
- `POST /ai_assistant/chat`：发起 AI 流式对话，返回旅游路线建议
- `POST /ai_assistant/get_history`：获取会话历史，支持多轮追溯
- `POST /ai_assistant/session_list`：获取历史会话列表，分页展示

> 详细参数与返回格式请参考代码注释和 DTO 类。

---

## 🔒 认证与权限机制

- 基于 Sa-Token + JWT，所有敏感接口需携带 Bearer Token
- 注解式权限控制（如 `@SaCheckLogin`, `@SaCheckPermission`）
- 支持多角色、细粒度权限分配
- 密码加密存储（BCrypt）
- 刷新令牌机制，提升安全性与体验
- 权限与角色数据通过数据库灵活配置，支持动态调整

---

## 🤖 AI 能力集成与多轮对话

- 支持 OpenAI、MCP 工具、HuggingFace、VertexAI 等多种大模型
- AI 旅游助手具备：
  - 天气查询、景点推荐、路线规划
  - 智能裁剪与摘要，自动结构化输出
  - 工具调用与异常兜底
- 多轮对话：
  - 每次对话均带有 sessionId，短期记忆在内存，长期记忆（历史会话、消息）存储于数据库
  - 支持上下文追溯，提升对话连贯性
- MCP 工具热插拔：
  - 工具注册与启用通过配置文件灵活管理
  - 支持未来扩展更多 AI 工具
- Token 超限防护：
  - JDK 动态代理拦截输出，ToolProvider 包装器二次截断
  - 智能摘要，保障响应内容不超限

---

## 🛫 部署与运行

1. 安装 JDK 21、Maven、MySQL 8.x
2. 初始化数据库（执行 `sql/create_table.sql`）
3. 配置 `application.yml` 数据库、AI、Sa-Token 等参数
4. 构建并运行：
   ```bash
   mvn clean package
   java -jar target/ai-tourism-0.0.1-SNAPSHOT.jar
   ```
5. 前端请参考 [ai-tourism-frontend 仓库](https://github.com/1937983507/ai-tourism-frontend)

---

## 🖼️ 示例效果

> ![请在此处补充前端界面截图1](screenshot1.png)
> ![请在此处补充前端界面截图2](screenshot2.png)

---

## 📬 联系与贡献

欢迎任何建议、反馈与贡献！如需交流或有合作意向，欢迎通过以下方式联系：

- 微信：vx13859211947
- 提交 Issue 或 PR 到本仓库
- 也欢迎访问前端项目：[ai-tourism-frontend 仓库](https://github.com/1937983507/ai-tourism-frontend)

如有 Bug、需求或想法，欢迎随时提出，我们会积极响应。

---

## 📝 License

本项目仅供学习使用，禁止未经授权的商用。
