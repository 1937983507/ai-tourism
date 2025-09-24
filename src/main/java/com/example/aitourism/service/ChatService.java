package com.example.aitourism.service;

import com.example.aitourism.dto.*;
import com.example.aitourism.entity.Message;
import com.example.aitourism.entity.Session;
import com.example.aitourism.mapper.ChatMessageMapper;
import com.example.aitourism.mapper.SessionMapper;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class ChatService {

    private final ChatMessageMapper chatMessageMapper;
    private final SessionMapper sessionMapper;

    private final AssistantService assistantService;

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

    public ChatService(ChatMessageMapper chatMessageMapper, SessionMapper sessionMapper, AssistantService assistantService) {
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


    /**
     * 发起对话
     */
    public String chat(String sessionId, String messages, Boolean stream, HttpServletResponse response) throws Exception {
        logger.info("用户的问题是："+messages);

        // 异步生成会话标题
        CompletableFuture<String> titleFuture = getTitleAsync(messages);
        String title = titleFuture.get(10, SECONDS);  // 超时10秒，避免长时间阻塞

        // 如果 session 不存在，则创建
        Session session = sessionMapper.findBySessionId(sessionId);
        if (session == null) {
            session = new Session();
            session.setSessionId(sessionId);
            session.setUserName("default_user");
            session.setTitle(title.length() > 10 ? title.substring(0, 10) : title);
            sessionMapper.insert(session);
        }

        // 保存用户消息
        Message userMsg = new Message();
        userMsg.setMsgId(UUID.randomUUID().toString());
        userMsg.setSessionId(sessionId);
        userMsg.setUserName("default_user");
        userMsg.setRole("user");
        userMsg.setTitle(session.getTitle());
        userMsg.setContent(messages);
        chatMessageMapper.insert(userMsg);

        // LLM 回复的内容
        final StringBuilder reply = new StringBuilder();

        if(!stream){
             System.out.println("非流式返回");
             reply.append("这是针对[").append(messages).append("]的返回内容");
        }else{
             System.out.println("流式返回");

            TokenStream tokenStream = assistantService.chat_Stream(sessionId, messages);

            // Set response type for event-stream
            response.setContentType("text/event-stream");
            response.setCharacterEncoding("UTF-8");

            // Write response using PrintWriter
            PrintWriter out = response.getWriter();

            // CompletableFuture to handle completion
            CompletableFuture<ChatResponse> futureResponse = new CompletableFuture<>();

            // Handle the token response and format it as per OpenAI's event-stream protocol
            tokenStream.onPartialResponse(token -> {
                        // Construct the event stream data for each token received
                        String tokenData = String.format(
                                "data: {\"choices\":[{\"index\":0,\"text\":\"%s\",\"finish_reason\":\"%s\",\"model\":\"%s\"}]}%n",
                                token.replace("\n", "\\n"), "stop", modelName
                        );
                        // 拼接LLM回复字符串
                        reply.append(token.replace("\n", "\\n"));
                        out.write(tokenData);
                        out.flush();
                    })
                    .onCompleteResponse(futureResponse::complete)
                    .onError(futureResponse::completeExceptionally)
                    .start();

            // Wait for the response to complete
            futureResponse.get(300, SECONDS);

            // Indicate the end of the stream to the client
            out.write("data: {\"choices\":[{\"finish_reason\":\"stop\"}]}");
            out.flush();

            // 生成路线结构体对象
            CompletableFuture<String> dailyRoutesFuture  = getDailyRoutes(reply.toString());
            String dailyRoutes = dailyRoutesFuture.get(10, SECONDS);  // 超时10秒，避免长时间阻塞

            // 更新数据库
            sessionMapper.updateRoutine(dailyRoutes, sessionId);

//            out.write(dailyRoutes);
//            out.flush();
        }

        // 构建消息，准备存入数据库中
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


    // 异步生成标题
    public CompletableFuture<String> getTitleAsync(String message){
        return CompletableFuture.supplyAsync(() -> {
            OpenAiChatModel model = OpenAiChatModel.builder()
                    .apiKey(apiKey)
                    .baseUrl(baseUrl)
                    .modelName(modelName)
                    .build();
            // 提示词模板
            String template = "请根据用户以下的问题生成一个会话标题，注意需要严格限制字数在8个中文字以内！用户问题为:{{problem}} ";
            PromptTemplate promptTemplate = PromptTemplate.from(template);
            // 填充变量(这里应该是用户自己输入的)
            Map<String, Object> variables = new HashMap<>();
            variables.put("problem", message);
            // 生成提示题
            Prompt prompt = promptTemplate.apply(variables);
            // 发起请求，并返回
            return model.chat(prompt.text());
        });
    }

    // 异步生成路线对象
    public CompletableFuture<String> getDailyRoutes(String reply){
        return CompletableFuture.supplyAsync(() -> {

            // 构建 JSON Schema
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

            // 构建 JSON Schema
            ResponseFormat responseFormat = ResponseFormat.builder()
                    .type(JSON)
                    .jsonSchema(JsonSchema.builder()
                            .name("RoutePlanner")
                            .rootElement(root)
                            .build())
                    .build();

            // 构建模型
            OpenAiChatModel model = OpenAiChatModel.builder()
                    .apiKey(apiKey)
                    .baseUrl(baseUrl)
                    .responseFormat(ResponseFormat.builder().type(JSON).build())
                    .modelName(modelName)
                    .build();

            // 提示词模板
            String template = """
                        ## 角色与任务
                        你是一个智能助手，我需要你基于一段旅游攻略，对其生成一个结构化对象，以表示多天内的路线途径点。
                        请注意，一定要注意其顺序性，各个地点之间的顺序必须严格遵守原文。

       
                        ## 当前输入旅游攻略
                        {{reply}}
                    """;

            PromptTemplate promptTemplate = PromptTemplate.from(template);
            // 填充变量(这里应该是用户自己输入的)
            Map<String, Object> variables = new HashMap<>();
            variables.put("reply", reply);
            // 生成提示题
            Prompt prompt = promptTemplate.apply(variables);
            // 构建请求
            ChatRequest chatRequest = ChatRequest.builder()
                    .responseFormat(responseFormat)
                    .messages(new UserMessage(prompt.text()))
                    .build();
            // 发起请求，并返回
            ChatResponse chatResponse = model.chat(chatRequest);

            System.out.println(reply);
            System.out.println(chatResponse.aiMessage().text());

            return chatResponse.aiMessage().text();
        });
    }





    /**
     * 获取会话历史
     */
    public List<ChatMessageDTO> getHistory(String sessionId) {
        List<Message> messages = chatMessageMapper.findBySessionId(sessionId);
        List<ChatMessageDTO> result = new ArrayList<>();
        for (Message m : messages) {
            ChatMessageDTO dto = new ChatMessageDTO(m.getMsgId(), m.getRole(), m.getContent());
            result.add(dto);
        }
        return result;
    }

    /**
     * 获取会话列表
     */
    public SessionListResponse getSessionList(Integer page, Integer pageSize) {
        int offset = (page - 1) * pageSize;
        List<Session> list = sessionMapper.findAll(offset, pageSize);
        int total = sessionMapper.count();

        List<SessionDTO> dtoList = new ArrayList<>();
        for (Session s : list) {
            // 裁剪传输字段，只保留有用的部分
            SessionDTO dto = new SessionDTO(s.getSessionId(), s.getModifyTime().toString(), s.getTitle(), s.getDailyRoutes());
            dtoList.add(dto);
        }

        SessionListResponse resp = new SessionListResponse();
        resp.setSessionList(dtoList);
        resp.setPage(page);
        resp.setPageSize(pageSize);
        resp.setTotal(total);
        return resp;
    }
}