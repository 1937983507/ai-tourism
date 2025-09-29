package com.example.aitourism.ai.truncator;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 用于在调用 StreamingChatModel 的 chat 方法前，对入参消息做安全裁剪。
 */
public final class ModelTrimmingProxies {

    private ModelTrimmingProxies() {}
    private static final Logger log = LoggerFactory.getLogger(ModelTrimmingProxies.class);

    /**
     * 使用 JDK 动态代理包装 {@link StreamingChatModel}。
     *
     * @param delegate   真实的流式模型实现（被代理对象）
     * @param enabled    是否启用裁剪（false 时直接返回原对象）
     * @param maxLen     单条文本最大长度上限（按字符），过长将被裁剪
     * @return           代理后的 StreamingChatModel（外观与原接口一致）
     */
    public static StreamingChatModel wrapStreaming(StreamingChatModel delegate, boolean enabled, int maxLen) {
        if (!enabled || delegate == null) return delegate;
        final int limit = Math.max(50, maxLen);

        // InvocationHandler 是 JDK 动态代理的“拦截器”：所有接口方法调用都会先进入这里
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                // 仅在调用 chat(...) 且首参为 List<ChatMessage> 或 ChatRequest 时进行裁剪逻辑，其余方法原样转发
                if ("chat".equals(method.getName()) && args != null && args.length > 0) {
                    // log.info("开始裁剪，消息是：");
                    Object first = args[0];
                    log.info(first.toString());
                    if (first instanceof List) {
                        // log.info("准备裁剪");
                        @SuppressWarnings("unchecked")
                        List<ChatMessage> messages = (List<ChatMessage>) first;
                        // log.info("裁剪前的消息", messages);
                        List<ChatMessage> trimmed = new ArrayList<>(messages.size());
                        for (ChatMessage m : messages) {
                            // log.info("当前消息：", m);
                            if (m instanceof UserMessage) {
                                // log.info("当前是用户消息");
                                String text = tryGetText(m);
                                if (text != null) {
                                    String t = McpResultTruncator.truncateResult(text, limit);
                                    // if (log.isInfoEnabled() && text.length() != t.length()) {
                                    //     log.info("[Trim:UserMessage] originalLen={}, trimmedLen={}, limit={}", text.length(), t.length(), limit);
                                    //     log.info("[Trim:UserMessage] originalPreview='{}'", abbreviate(text, 160));
                                    //     log.info("[Trim:UserMessage] trimmedPreview='{}'", abbreviate(t, 160));
                                    // }
                                    trimmed.add(UserMessage.from(t));
                                } else {
                                    trimmed.add(m);
                                }
                            } else if (m instanceof ToolExecutionResultMessage) {
                                // log.info("当前是工具消息");
                                String text = tryGetText(m);
                                if (text != null) {
                                    String t = McpResultTruncator.truncateResult(text, limit);
                                    // 由于不同版本构造器差异，这里保留原消息以确保兼容
                                    // if (log.isInfoEnabled() && text.length() != t.length()) {
                                    //     log.info("[Trim:ToolExecutionResult] originalLen={}, trimmedLen={}, limit={}", text.length(), t.length(), limit);
                                    //     log.info("[Trim:ToolExecutionResult] originalPreview='{}'", abbreviate(text, 160));
                                    //     log.info("[Trim:ToolExecutionResult] trimmedPreview='{}'", abbreviate(t, 160));
                                    // }
                                    trimmed.add(m);
                                } else {
                                    trimmed.add(m);
                                }
                            } else if (m instanceof SystemMessage || m instanceof AiMessage) {
                                // log.info("当前是系统AI机器人的消息");
                                trimmed.add(m);
                            } else {
                                trimmed.add(m);
                            }
                        }
                        Object[] newArgs = args.clone();
                        newArgs[0] = trimmed;
                        // 将新参数转发给真实实现（delegate）
                        return method.invoke(delegate, newArgs);
                    } else if (first instanceof ChatRequest) {
                        // log.info("fist 果然是ChatRequest类型");
                        ChatRequest req = (ChatRequest) first;
                        List<ChatMessage> messages = req.messages();
                        if (messages != null) {
                            // log.info("准备裁剪 ChatRequest.messages");
                            List<ChatMessage> trimmed = new ArrayList<>(messages.size());
                            for (ChatMessage m : messages) {
                                if (m instanceof UserMessage) {
                                    String text = tryGetText(m);
                                    if (text != null) {
                                        String t = McpResultTruncator.truncateResult(text, limit);
                                        // if (log.isInfoEnabled() && text.length() != t.length()) {
                                        //     log.info("[Trim:UserMessage] originalLen={}, trimmedLen={}, limit={}", text.length(), t.length(), limit);
                                        //     log.info("[Trim:UserMessage] originalPreview='{}'", abbreviate(text, 160));
                                        //     log.info("[Trim:UserMessage] trimmedPreview='{}'", abbreviate(t, 160));
                                        // }
                                        trimmed.add(UserMessage.from(t));
                                    } else {
                                        trimmed.add(m);
                                    }
                                } else if (m instanceof ToolExecutionResultMessage) {
                                    String text = tryGetText(m);
                                    if (text != null) {
                                        String t = McpResultTruncator.truncateResult(text, limit);
                                        // if (log.isInfoEnabled() && text.length() != t.length()) {
                                        //     log.info("[Trim:ToolExecutionResult] originalLen={}, trimmedLen={}, limit={}", text.length(), t.length(), limit);
                                        //     log.info("[Trim:ToolExecutionResult] originalPreview='{}'", abbreviate(text, 160));
                                        //     log.info("[Trim:ToolExecutionResult] trimmedPreview='{}'", abbreviate(t, 160));
                                        // }
                                        // 由于不同版本构造器差异，这里保留原消息以确保兼容
                                        trimmed.add(m);
                                    } else {
                                        trimmed.add(m);
                                    }
                                } else {
                                    trimmed.add(m);
                                }
                            }
                            // 重建一个仅替换 messages 的 ChatRequest（保持其它字段默认/为空以确保兼容）
                            ChatRequest newReq = ChatRequest.builder()
                                    .messages(trimmed)
                                    .build();
                            Object[] newArgs = args.clone();
                            newArgs[0] = newReq;
                            return method.invoke(delegate, newArgs);
                        }
                    }
                }
                // 非 chat(...) 或参数不匹配时，直接透明转发
                return method.invoke(delegate, args);
            }
        };

        // JDK 动态代理：基于接口生成代理对象
        return (StreamingChatModel) Proxy.newProxyInstance(
                delegate.getClass().getClassLoader(),
                new Class[]{StreamingChatModel.class},
                handler
        );
    }

    /**
     * 通过反射尝试调用 text() 读取消息文本；若不可用则返回 null。
     * 之所以使用反射，是为了兼容不同版本的 langchain4j 对消息对象的访问 API 差异。
     */
    private static String tryGetText(Object message) {
        try {
            Method m = message.getClass().getMethod("text");
            Object val = m.invoke(message);
            return val instanceof String ? (String) val : null;
        } catch (Exception ignore) {
            return null;
        }
    }

    private static String abbreviate(String s, int max) {
        if (s == null) return null;
        if (s.length() <= max) return s;
        int head = Math.min(80, Math.max(0, max - 20));
        int tail = Math.max(0, max - head - 3);
        String start = s.substring(0, head);
        String end = tail > 0 ? s.substring(s.length() - tail) : "";
        return start + "..." + end;
    }
}


