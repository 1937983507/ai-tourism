package com.example.aitourism.service;

import com.example.aitourism.entity.Message;
import com.example.aitourism.entity.Session;
import com.example.aitourism.mapper.ChatMessageMapper;
import com.example.aitourism.mapper.SessionMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.*;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.TokenStream;
//import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.memory.ChatMemoryAccess;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import dev.langchain4j.model.chat.StreamingChatModel;

import dev.langchain4j.data.message.ChatMessage;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
//@Lazy
//@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class AssistantService {

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.base-url}")
    private String baseUrl;

    @Value("${openai.model-name}")
    private String modelName;

    private final McpClientService mcpClientService;

    private static final Logger logger = LoggerFactory.getLogger(AssistantService.class);

    private final ChatMessageMapper chatMessageMapper;
    private final SessionMapper sessionMapper;

    private Assistant assistant;

//    @PostConstruct
//    public void init() {
//        OpenAiChatModel model = OpenAiChatModel.builder()
//                .apiKey(apiKey)
//                .baseUrl(baseUrl)
//                .modelName(modelName)
//                .build();
//        System.out.println("init的model创建成功");
//
//        assistant = AiServices.builder(Assistant.class)
//                .chatModel(model)
//                .build();
//        System.out.println("init的assistant创建成功");
//    }

    @PostConstruct
    public void init_stream() {

        System.out.println("开始初始化 init");

//        PersistentChatMemoryStore store = new PersistentChatMemoryStore();
//
//        ChatMemoryProvider chatMemoryProvider = memoryId -> MessageWindowChatMemory.builder()
//                .id(memoryId)
//                .maxMessages(10)
//                .chatMemoryStore(store)
//                .build();

        StreamingChatModel streamingModel = OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .build();

        try {
            assistant = AiServices.builder(Assistant.class)
                    .streamingChatModel(streamingModel)
                    .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
//                    .chatMemoryProvider(chatMemoryProvider)
                    .toolProvider(mcpClientService.createToolProvider())
                    .build();
        } catch (Exception e) {
            // 可以在这里添加重试逻辑
            // System.out.println("创建assistance失败："+e.getMessage());
            throw new RuntimeException("创建assistance失败：", e);
        }
    }

//    public String chat(String message) {
//        System.out.println("进入非流式的chat");
//        // 延迟初始化，确保在第一次使用时创建
//        if (assistant == null) {
//            init();
//        }
//
//        try {
//            return assistant.chat(message);
//        } catch (Exception e) {
//            // 可以在这里添加重试逻辑
//            throw new RuntimeException("Chat service unavailable", e);
//        }
//    }

    public TokenStream chat_Stream(String memoryId, String message) {
        // 延迟初始化，确保在第一次使用时创建
        if (assistant == null) {
            init_stream();
        }

        try {
            // 开始发起流式请求
            return assistant.chat_Stream(memoryId, message);
        } catch (Exception e) {
            // 可以在这里添加重试逻辑
            throw new RuntimeException("Chat service unavailable", e);
        }
    }



    // 定义Assistant接口
    public interface Assistant{
        @dev.langchain4j.service.SystemMessage(
            """
                ## 角色
                你是一个智能旅游规划助手，能够为用户提供指定范围的旅游攻略。
                
                ## 任务
                若是用户需要某城市的旅游攻略，则进行如下流程：
                - 第一步，首先获取该城市近几天的天气预报，
                - 第二步，按照用户喜好，调用搜索引擎获取指定范围内的著名景点信息，一些热门景点最好有对应的附图链接。
                （注意，若是有给出附图，则一定仔细校验，避免出现图文不符的问题！）
                - 第三步，最后基于以上信息，分析给出用户所适合的旅游攻略。
                
                ## 输出
                - 第一，首先输出该城市未来几天的天气情况，给出出行建议（例如要带伞/防晒等），
                - 第二，然后将旅游攻略按天给出，例如第1天、第2天、等等。
            """)

        TokenStream chat_Stream(@MemoryId String memoryId, @dev.langchain4j.service.UserMessage String userMessage);

        String chat(String userMessage);
    }


    // You can create your own implementation of ChatMemoryStore and store chat memory whenever you'd like
    class PersistentChatMemoryStore implements ChatMemoryStore {

//        private final DB db = DBMaker.fileDB("multi-user-chat-memory.db").transactionEnable().make();
//        private final Map<Integer, String> map = db.hashMap("messages", INTEGER, STRING).createOrOpen();


//        private final ObjectMapper objectMapper;
//        private final SessionMapper sessionMapper;
//        private final ChatMessageMapper chatMessageMapper;

        public PersistentChatMemoryStore() {}

        @Override
        public List<ChatMessage> getMessages(Object memoryId) {
            // 从数据库按sessionId查询所有消息，并转换为LangChain4j的ChatMessage
//            List<Message> dbMessages = chatMessageMapper.findBySessionId(memoryId.toString());
//            return convertToL4jMessages(dbMessages);
            return null;
        }

        @Override
        public void updateMessages(Object memoryId, List<ChatMessage> messages) {
            // 根据memoryId将消息列表更新到数据库
//            String json = messagesToJson(messages);
//            map.put((int) memoryId, json);
//            db.commit();


            // 先删除该session的所有旧消息
            // 注意：这里需要您补充删除方法到Mapper中
            // chatMessageMapper.deleteBySessionId(sessionId);


//            // 将LangChain4j的ChatMessage转换为数据库实体并保存
//            for (int i = 0; i < messages.size(); i++) {
//                ChatMessage l4jMessage = messages.get(i);
//                Message dbMessage = convertToDbMessage(memoryId.toString(), l4jMessage, i);
//                chatMessageMapper.insert(dbMessage);
//            }

//            // 获取当前数据库中已有的消息数量
//            int existingCount = chatMessageMapper.countBySessionId(memoryId.toString());
//
//            // 只保存新消息（从existingCount开始）
//            for (int i = existingCount; i < messages.size(); i++) {
//                ChatMessage l4jMessage = messages.get(i);
//                Message dbMessage = convertToDbMessage(memoryId.toString(), l4jMessage, i);
//                chatMessageMapper.insert(dbMessage);
//            }

        }


        @Override
        public void deleteMessages(Object memoryId) {
            // 根据memoryId删除消息
//            map.remove((int) memoryId);
//            db.commit();
        }

        private List<ChatMessage> convertToL4jMessages(List<Message> dbMessages) {
            return dbMessages.stream().map(dbMessage -> {
                switch (dbMessage.getRole().toLowerCase()) {
                    case "user":
                        return new UserMessage(dbMessage.getContent());
                    case "assistant":
                        return new AiMessage(dbMessage.getContent());
                    case "system":
                        return new SystemMessage(dbMessage.getContent());
                    default:
                        return new UserMessage(dbMessage.getContent());
                }
            }).collect(Collectors.toList());
        }

        private Message convertToDbMessage(String sessionId, ChatMessage l4jMessage, int index) {
            Session session = sessionMapper.findBySessionId(sessionId);

            Message dbMessage = new Message();
            dbMessage.setMsgId(generateMsgId(sessionId, index));
            dbMessage.setSessionId(sessionId);
            dbMessage.setRole(getRoleFromMessage(l4jMessage));
            dbMessage.setContent(getContentFromMessage(l4jMessage));
            dbMessage.setTitle(session.getTitle()); // 可根据内容生成更有意义的标题
            return dbMessage;
        }

        private String getContentFromMessage(ChatMessage message) {
            // 方法1：使用类型检查
            if (message instanceof UserMessage) {
                return ((UserMessage) message).singleText();
            } else if (message instanceof AiMessage) {
                return ((AiMessage) message).text();
            } else if (message instanceof SystemMessage) {
                return ((SystemMessage) message).text();
            } else if (message instanceof ToolExecutionResultMessage) {
                return ((ToolExecutionResultMessage) message).text();
            }

            // 方法2：使用toString()作为备选（可能包含额外信息）
            return message.toString();
        }

        private String getRoleFromMessage(ChatMessage message) {
            if (message instanceof UserMessage) return "user";
            if (message instanceof AiMessage) return "assistant";
            if (message instanceof SystemMessage) return "system";
            return "user";
        }

        private String generateMsgId(String sessionId, int index) {
            return UUID.randomUUID().toString();
//            return sessionId + "_" + System.currentTimeMillis() + "_" + index;
        }
    }

}