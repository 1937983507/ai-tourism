package com.example.aitourism.monitor;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class MonitorContextHolder {

    private static final ThreadLocal<MonitorContext> CONTEXT_HOLDER = new ThreadLocal<>();
    // 当回调在线程池的其他线程执行时，ThreadLocal 无法传递；提供一次性全局后备以便监听器读取
    private static volatile MonitorContext TEMP_FALLBACK_CONTEXT;

    /**
     * 设置监控上下文
     */
    public static void setContext(MonitorContext context) {
        CONTEXT_HOLDER.set(context);
        TEMP_FALLBACK_CONTEXT = context;
    }

    /**
     * 获取当前监控上下文
     */
    public static MonitorContext getContext() {
        return CONTEXT_HOLDER.get();
    }

    /**
     * 获取一次性全局后备上下文，并清空该后备，避免串扰
     */
    public static MonitorContext pollFallbackContext() {
        MonitorContext ctx = TEMP_FALLBACK_CONTEXT;
        TEMP_FALLBACK_CONTEXT = null;
        return ctx;
    }

    /**
     * 清除监控上下文
     */
    public static void clearContext() {
        CONTEXT_HOLDER.remove();
    }
}
