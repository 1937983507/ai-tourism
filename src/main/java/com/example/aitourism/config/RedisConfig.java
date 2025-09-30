package com.example.aitourism.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.example.aitourism.ai.memory.CustomRedisChatMemoryStore;

import dev.langchain4j.store.memory.chat.ChatMemoryStore;

import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.beans.factory.annotation.Value;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;


@Configuration
@ConfigurationProperties(prefix = "ai.memory.redis")
@Data
@Slf4j
public class RedisConfig {

     /** 记忆键过期时间（秒） */
     @Value("${ttl:1800}")
     private long ttl;
     /** Redis 键前缀，便于隔离/清理 */
     private String keyPrefix = "ai:memory:";
 
    // /**
    //  * 提供对话记忆存储（自定义实现）：
    // * - 使用普通 String 存储完整 JSON 文本
    // * - 支持 key 前缀与过期时间
    // */
    // @Bean
    // @Primary
    // public ChatMemoryStore chatMemoryStore(RedisTemplate<String, Object> redisTemplate) {
    //     log.info("初始化 CustomRedisChatMemoryStore, prefix={}, ttl={}s", keyPrefix, ttl);
    //     // CustomRedisChatMemoryStore 是 chatMemoryStore 接口的实现类，在其中实现了基于Redis管理消息
    //     return new CustomRedisChatMemoryStore(redisTemplate, keyPrefix, ttl);
    // }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        
        // 使用 String 序列化器作为 key 的序列化器
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // 使用 GenericJackson2JsonRedisSerializer 作为 value 的序列化器
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        
        template.afterPropertiesSet();
        return template;
    }

}