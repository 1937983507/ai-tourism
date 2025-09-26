package com.example.aitourism.service.impl;

import com.example.aitourism.dto.*;
import com.example.aitourism.entity.Message;
import com.example.aitourism.entity.Session;
import com.example.aitourism.mapper.ChatMessageMapper;
import com.example.aitourism.mapper.SessionMapper;
import com.example.aitourism.service.ChatService;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.chat.request.json.JsonSchemaElement;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.TokenStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static dev.langchain4j.model.chat.request.ResponseFormatType.JSON;
import static java.util.concurrent.TimeUnit.SECONDS;

import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.data.message.UserMessage;

@Service
@Slf4j
public class ChatServiceImpl implements ChatService {

    private final ChatMessageMapper chatMessageMapper;
    private final SessionMapper sessionMapper;
    private final AssistantService assistantService;

    public ChatServiceImpl(ChatMessageMapper chatMessageMapper, SessionMapper sessionMapper, AssistantService assistantService) {
        this.chatMessageMapper = chatMessageMapper;
        this.sessionMapper = sessionMapper;
        this.assistantService = assistantService;
    }

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.base-url}")
    private String baseUrl;

    @Value("${openai.model-name}")
    private String modelName;

    @Override
    public String chat(String sessionId, String messages, String userId, Boolean stream, HttpServletResponse response) throws Exception {
        log.info("用户的问题是：{}", messages);

        // 在请求 LLM 前确保 Assistant 与 MCP 工具就绪
        boolean ready = assistantService.ensureReady();
        if (!ready) {
            throw new RuntimeException("Assistant/MCP 工具不可用，请稍后重试");
        }

        String title = messages;

        Session session = sessionMapper.findBySessionId(sessionId);
        if (session == null) {
            session = new Session();
            session.setSessionId(sessionId);
            session.setUserName("default_user");
            session.setTitle(title.length() > 10 ? title.substring(0, 10) : title);
            session.setUserId(userId);
            sessionMapper.insert(session);
        }

        Message userMsg = new Message();
        userMsg.setMsgId(UUID.randomUUID().toString());
        userMsg.setSessionId(sessionId);
        userMsg.setUserName("default_user");
        userMsg.setRole("user");
        userMsg.setTitle(session.getTitle());
        userMsg.setContent(messages);
        chatMessageMapper.insert(userMsg);

        final StringBuilder reply = new StringBuilder();

        if(!stream){
             log.info("非流式返回");
             reply.append("这是针对[").append(messages).append("]的返回内容");
        }else{
            log.info("流式返回");

            TokenStream tokenStream = assistantService.chat_Stream(sessionId, messages);

            response.setContentType("text/event-stream");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Cache-Control", "no-cache");

            PrintWriter out = response.getWriter();

            CompletableFuture<ChatResponse> futureResponse = new CompletableFuture<>();

            tokenStream.onPartialResponse(token -> {
                        String esc = token.replace("\n", "\\n");
                        log.debug("SSE token chunk: {}", esc);
                        String tokenData = String.format(
                                "data: {\"choices\":[{\"index\":0,\"text\":\"%s\",\"finish_reason\":\"%s\",\"model\":\"%s\"}]}%n%n",
                                esc, "stop", modelName
                        );
                        reply.append(esc);
                        out.write(tokenData);
                        out.flush();
                    })
                    .onCompleteResponse(futureResponse::complete)
                    .onError(error -> {
                        futureResponse.completeExceptionally(error);
                        try {
                            String refined = refineErrorMessage(error).replace("\n", "\\n");
                            String errEvent = String.format(
                                "data: {\"choices\":[{\"index\":0,\"text\":\"%s\",\"finish_reason\":\"%s\",\"model\":\"%s\"}]}%n%n",
                                refined, "stop", modelName
                            );
                            reply.append(refined);
                            out.write(errEvent);
                            out.flush();
                        } catch (Exception ignored) {}
                    })
                    .start();

            try {
                futureResponse.get(300, SECONDS);
            } catch (Exception ex) {
                log.error("SSE streaming failed: {}", ex.getMessage(), ex);
            }

            out.write("data: {\"choices\":[{\"finish_reason\":\"stop\"}]}\n\n");
            out.flush();

        }

        log.info("模型给出的路线规划：{}", reply);
        log.info("开始生成路线结构体对象");

        CompletableFuture<String> dailyRoutesFuture  = getDailyRoutes(reply.toString());
        String dailyRoutes = dailyRoutesFuture.get(10, SECONDS);
        log.info("模型给出的路线结构体对象："+dailyRoutes);
        sessionMapper.updateRoutine(dailyRoutes, sessionId);

        Message assistantMsg = new Message();
        assistantMsg.setMsgId(UUID.randomUUID().toString());
        assistantMsg.setSessionId(sessionId);
        assistantMsg.setUserName("assistant");
        assistantMsg.setRole("assistant");
        assistantMsg.setTitle(session.getTitle());
        assistantMsg.setContent(reply.toString());
        chatMessageMapper.insert(assistantMsg);

        return reply.toString();
    }

    private String refineErrorMessage(Throwable error) {
        if (error == null) {
            return "服务暂不可用，请稍后重试";
        }
        String msg = String.valueOf(error.getMessage());
        if (msg != null && (msg.contains("免费API限制模型输入token小于4096") || msg.contains("prompt tokens") || msg.contains("4096") || msg.contains("FORBIDDEN"))) {
            return "十分抱歉，免费API对模型输入有4096 token上限。";
        }
        return "对话服务暂时出现波动，请稍后再试";
    }

    // 异步生成标题
    private CompletableFuture<String> getTitleAsync(String message){
        return CompletableFuture.supplyAsync(() -> {
            OpenAiChatModel model = OpenAiChatModel.builder()
                    .apiKey(apiKey)
                    .baseUrl(baseUrl)
                    .modelName(modelName)
                    .build();
            String template = "请根据用户以下的问题生成一个会话标题，注意需要严格限制字数在8个中文字以内！用户问题为:{{problem}} ";
            PromptTemplate promptTemplate = PromptTemplate.from(template);
            Map<String, Object> variables = new HashMap<>();
            variables.put("problem", message);
            Prompt prompt = promptTemplate.apply(variables);
            return model.chat(prompt.text());
        });
    }

    // 异步生成路线对象
    private CompletableFuture<String> getDailyRoutes(String reply){
        return CompletableFuture.supplyAsync(() -> {

            JsonSchemaElement root = JsonObjectSchema.builder()
                    .description("完整的路线规划")
                    .addProperty("dailyRoutes", JsonArraySchema.builder()
                            .description("多天的路线规划数组")
                            .items(JsonObjectSchema.builder()
                                    .description("某一天的路线规划")
                                    .addProperty("points", JsonArraySchema.builder()
                                            .description("当天的多个地点")
                                            .items(JsonObjectSchema.builder()
                                                    .description("某一地点/景点的属性信息")
                                                    .addStringProperty("keyword", "地点名/景点名")
                                                    .addStringProperty("city", "所属城市")
                                                    .build())
                                            .build())
                                    .build())
                            .build())
                    .build();

            ResponseFormat responseFormat = ResponseFormat.builder()
                    .type(JSON)
                    .jsonSchema(JsonSchema.builder()
                            .name("RoutePlanner")
                            .rootElement(root)
                            .build())
                    .build();

            OpenAiChatModel model = OpenAiChatModel.builder()
                    .apiKey(apiKey)
                    .baseUrl(baseUrl)
                    .responseFormat(ResponseFormat.builder().type(JSON).build())
                    .modelName(modelName)
                    .build();

            String template = """
                        ## 角色与任务
                        你是一个智能助手，我需要你基于一段旅游攻略，对其生成一个结构化对象，以表示多天内的路线途径点。

                        ## 示例输入
                        3天2夜旅游攻略\\n\\n#### 第1天：探访文化和购物中心\\n- **上午**：前往**大鹏所城文化旅游区**，了解深圳的历史和文化，欣赏古建筑和自然风光。\\n- **中午**：在大鹏附近的当地餐馆享用海鲜午餐。\\n- **下午**：游览**深圳博物馆**，了解深圳的发展历程和文化。\\n- **晚上**：前往**东门老街**，体验深圳的夜市文化，晚餐可以选择当地美食小吃。\\n\\n#### 第2天：自然与体验之旅\\n- **上午**：前往**深圳湾公园**，享受海边的自然风光，可以骑自行车或者步行。\\n- **中午**：在公园内附近的餐厅就餐，享受海鲜或地方特色菜。\\n- **下午**：参观**欢乐谷主题公园**，体验各种游乐设施，可以在这里待到晚上。\\n- **晚上**：在欢乐谷周边的餐馆用晚餐，结束一天的游玩。\\n\\n#### 第3天：现代化都市探索\\n- **上午**：参观**华强北电子市场**，这里是世界著名的电子产品市场，非常适合科技爱好者。\\n- **中午**：在华强北附近的餐馆用午餐，体验深圳的现代美食。\\n- **下午**：游览**深圳市内的各大摩天楼**如平安金融中心，欣赏城市全景。\\n- **晚上**：在**COCO Park**或**万象城**购物和就餐，体验深圳的时尚潮流。\\n\\n希望以上旅游攻略能为你的深圳之行提供帮助！如果有任何其他的需求，欢迎随时咨询。
                        
                        ## 示例输出
                        {"dailyRoutes":[{"points":[{"keyword":"大鹏所城文化旅游区","city":"深圳"},{"keyword":"深圳博物馆","city":"深圳"},{"keyword":"东门老街","city":"深圳"}]},{"points":[{"keyword":"深圳湾公园","city":"深圳"},{"keyword":"欢乐谷主题公园","city":"深圳"}]},{"points":[{"keyword":"华强北电子市场","city":"深圳"},{"keyword":"COCO Park","city":"深圳"},{"keyword":"万象城","city":"深圳"}]}]}
                        
                        ## 注意事项
                        1、一定要注意并保证其顺序性，各个地点之间的顺序必须严格遵守原文。
                        2、输出keyword字段是具体可定位到的地名，不能是餐馆之类泛称；输出city字段是城市名，例如深圳、广州、北京这种城市名

                        ## 当前输入原文
                        {{reply}}
                    """;

            PromptTemplate promptTemplate = PromptTemplate.from(template);
            Map<String, Object> variables = new HashMap<>();
            variables.put("reply", reply);
            Prompt prompt = promptTemplate.apply(variables);
            ChatRequest chatRequest = ChatRequest.builder()
                    .responseFormat(responseFormat)
                    .messages(new UserMessage(prompt.text()))
                    .build();
            ChatResponse chatResponse = model.chat(chatRequest);

            System.out.println(reply);
            System.out.println(chatResponse.aiMessage().text());

            return chatResponse.aiMessage().text();
        });
    }

    @Override
    public List<ChatMessageDTO> getHistory(String sessionId) {
        List<Message> messages = chatMessageMapper.findBySessionId(sessionId);
        List<ChatMessageDTO> result = new ArrayList<>();
        for (Message m : messages) {
            ChatMessageDTO dto = new ChatMessageDTO(m.getMsgId(), m.getRole(), m.getContent());
            result.add(dto);
        }
        return result;
    }

    @Override
    public SessionListResponse getSessionList(Integer page, Integer pageSize, String userId) {
        int offset = (page - 1) * pageSize;
        List<Session> list = sessionMapper.findByUserId(offset, pageSize, userId);
        int total = sessionMapper.count();

        List<SessionDTO> dtoList = new ArrayList<>();
        for (Session s : list) {
            SessionDTO dto = new SessionDTO(s.getSessionId(), s.getModifyTime().toString(), s.getTitle(), s.getDailyRoutes());
            dtoList.add(dto);
        }

        SessionListResponse resp = new SessionListResponse();
        resp.setSessionList(dtoList);
        resp.setPage(page);
        resp.setPageSize(pageSize);
        resp.setTotal(total);

        System.out.println("返回的sessionList："+resp);

        return resp;
    }
}


