package com.example.aitourism.service;

import com.example.aitourism.dto.*;
import com.example.aitourism.entity.Message;
import com.example.aitourism.entity.Session;
import com.example.aitourism.mapper.ChatMessageMapper;
import com.example.aitourism.mapper.SessionMapper;
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

import static java.util.concurrent.TimeUnit.SECONDS;

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


    // 根据用户问题生成会话标题
    public String getTitle(String message){
        OpenAiChatModel model = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .build();
        // 提示词模板
        String templete = "请根据用户以下的问题生成一个会话标题，注意需要严格限制字数在8个中文字以内！用户问题为:{{problem}} ";
        PromptTemplate promptTemplate = PromptTemplate.from(templete);
        // 填充变量(这里应该是用户自己输入的)
        Map<String, Object> variables = new HashMap<>();
        variables.put("problem", message);
        // 生成提示题
        Prompt prompt = promptTemplate.apply(variables);
        // 发起请求，并返回
        return model.chat(prompt.text());
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
            SessionDTO dto = new SessionDTO(s.getSessionId(), s.getModifyTime().toString(), s.getTitle());
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