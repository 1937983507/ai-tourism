package com.example.aitourism.ai.memory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.example.aitourism.mapper.ChatMessageMapper;

import static dev.langchain4j.data.message.ChatMessageDeserializer.messagesFromJson;
import static dev.langchain4j.data.message.ChatMessageSerializer.messagesToJson;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.ArrayList;
import java.time.Duration;


// 实现ChatMemoryStore接口，在其中基于Redis管理消息
// 只要实现了ChatMemoryStore接口，LangChain4j就会自动使用这个实现来管理消息
// 而不会关心其内部使用什么，例如可以用Redis或MySQL等等
@Slf4j
@Service
@ConfigurationProperties(prefix = "ai.memory.redis")
@RequiredArgsConstructor
@Primary
public class CustomRedisChatMemoryStore implements ChatMemoryStore {
    
    private final RedisTemplate<String, Object> redisTemplate;
    @Value("${ai.memory.redis.key-prefix:ai:memory:}")
    private String keyPrefix;
    @Value("${ai.memory.redis.ttl:1800}")
    private long ttlSeconds;
    private final ChatMessageMapper chatMessageMapper;

    /**
     * 自定义的基于 Redis 的对话记忆存储。
     * 适用于未安装 RedisJSON 模块的环境，使用普通 String value 存储 JSON 文本。
     * key 形如：{keyPrefix}{memoryId}，并为每条 key 设置过期时间（ttlSeconds）。
     */
    // @Autowired
    // public CustomRedisChatMemoryStore(RedisTemplate<String, Object> redisTemplate) {
    //     this.redisTemplate = redisTemplate;
    // }

    // /**
    //  * @param redisTemplate Spring 管理的 RedisTemplate
    //  * @param keyPrefix     Redis 键前缀，便于隔离/清理
    //  * @param ttlSeconds    过期时间（秒），控制对话上下文的生命周期
    //  */
    // public CustomRedisChatMemoryStore(RedisTemplate<String, Object> redisTemplate, String keyPrefix, long ttlSeconds) {
    //     this.redisTemplate = redisTemplate;
    //     this.keyPrefix = keyPrefix == null ? "chat-memory:" : keyPrefix;
    //     this.ttlSeconds = ttlSeconds > 0 ? ttlSeconds : 1800;
    // }

    /**
     * 组合 Redis 键：前缀 + memoryId
     */
    private String buildKey(Object memoryId) {
        return keyPrefix + memoryId;
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        // 从 Redis 读取 JSON 文本并反序列化为 ChatMessage 列表
        try {
            // log.info("开始从Redis中获取记忆");
            Object value = redisTemplate.opsForValue().get(buildKey(memoryId));
            if (value == null) {
                return new ArrayList<>();
            }
            String json = value instanceof String ? (String) value : String.valueOf(value);
            List<ChatMessage> messages = messagesFromJson(json);
            // log.info("从Redis中获取的记忆: {}", json);
            if(messages==null || messages.isEmpty() || isOnlySystemMessage(messages)){
                // 为空或只有系统消息（此时就是Redis超时过期），尝试从数据库中获取数据
                log.info("Redis中无有效记忆（空或仅系统消息），尝试从数据库获取历史消息");
                List<ChatMessage> tempMemory =  loadMessagesFromDatabase(memoryId);
                // 将内容写到Redis中
                updateMessages(memoryId, tempMemory);
                return tempMemory;
            } else {
                // 不为空且包含有效对话，返回Redis中的记忆
                log.info("成功从Redis获取到{}条记忆", messages.size());
                return messages;
            }
        } catch (Exception e) {
            log.warn("读取Redis记忆失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        // 将消息序列化为 JSON，并写回 Redis，同时设置过期时间
        try {
            // log.info("开始将以下记忆写入Redis：{}", messages);
            String json = messagesToJson(messages == null ? new ArrayList<>() : messages);
            redisTemplate.opsForValue().set(buildKey(memoryId), json, Duration.ofSeconds(ttlSeconds));
        } catch (Exception e) {
            log.warn("写入Redis记忆失败: {}", e.getMessage());
        }
    }

    @Override
    public void deleteMessages(Object memoryId) {
        // 删除该 memoryId 对应的 Redis 键
        try {
            log.info("开始删除Redis中某一key的记忆: {}", memoryId);
            redisTemplate.delete(buildKey(memoryId));
        } catch (Exception e) {
            log.warn("删除Redis记忆失败: {}", e.getMessage());
        }
    }

    /**
     * 判断是否只有系统消息
     */
    private boolean isOnlySystemMessage(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return true;
        }
        
        // 如果只有一条消息且是系统消息，则认为是无效记忆
        if (messages.size() == 1) {
            ChatMessage message = messages.get(0);
            return message instanceof dev.langchain4j.data.message.SystemMessage;
        }
        
        // 如果所有消息都是系统消息，也认为是无效记忆
        return messages.stream().allMatch(msg -> msg instanceof dev.langchain4j.data.message.SystemMessage);
    }

    /**
     * 从数据库加载历史消息作为记忆
     */
    private List<ChatMessage> loadMessagesFromDatabase(Object memoryId) {
        try {
            String sessionId = memoryId.toString();
            // log.info("从数据库加载会话 {} 的历史消息", sessionId);
            
            // 从数据库获取历史消息
            var dbMessages = chatMessageMapper.findBySessionId(sessionId);
            if (dbMessages == null || dbMessages.isEmpty()) {
                // log.info("数据库中也没有会话 {} 的历史消息", sessionId);
                return new ArrayList<>();
            }
            
            // 转换为LangChain4j的ChatMessage格式
            List<ChatMessage> l4jMessages = new ArrayList<>();
            for (var dbMessage : dbMessages) {
                switch (dbMessage.getRole().toLowerCase()) {
                    case "user":
                        l4jMessages.add(dev.langchain4j.data.message.UserMessage.from(dbMessage.getContent()));
                        break;
                    case "assistant":
                        l4jMessages.add(dev.langchain4j.data.message.AiMessage.from(dbMessage.getContent()));
                        break;
                    case "system":
                        l4jMessages.add(dev.langchain4j.data.message.SystemMessage.from(dbMessage.getContent()));
                        break;
                    default:
                        l4jMessages.add(dev.langchain4j.data.message.UserMessage.from(dbMessage.getContent()));
                        break;
                }
            }
            
            // log.info("从数据库成功加载了{}条历史消息", l4jMessages.size());
            
            // 将数据库消息回填到Redis中，避免下次重复查询
            if (!l4jMessages.isEmpty()) {
                try {
                    String json = messagesToJson(l4jMessages);
                    redisTemplate.opsForValue().set(buildKey(memoryId), json, Duration.ofSeconds(ttlSeconds));
                    // log.info("已将数据库消息回填到Redis");
                } catch (Exception e) {
                    log.warn("回填Redis失败: {}", e.getMessage());
                }
            }
            
            return l4jMessages;
        } catch (Exception e) {
            log.error("从数据库加载历史消息失败: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
}