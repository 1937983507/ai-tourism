package com.example.aitourism.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Enumeration;
import java.util.UUID;

@Component
public class LogInterceptor implements HandlerInterceptor {

    // 使用 SLF4J 日志框架
    // 创建Logger实例，参数为当前类，用于标识日志来源
    // 使用LoggerFactory获取Logger，这是SLF4J的标准做法
    private static final Logger logger = LoggerFactory.getLogger(LogInterceptor.class);
    private static final ThreadLocal<Long> startTimeThreadLocal = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
//        System.out.println("Interceptor 日志 -> " + request.getMethod() + " " + request.getRequestURI());
        // 存储请求开始时间
        startTimeThreadLocal.set(System.currentTimeMillis());

        // 生成唯一请求ID，用于追踪整个请求链路
        String requestId = UUID.randomUUID().toString();
        // 将请求ID放入MDC（Mapped Diagnostic Context），便于在日志中统一显示
        MDC.put("traceId", requestId);

        // 使用日志框架记录请求信息，可以方便地控制输出级别（DEBUG/INFO等）
        if (logger.isInfoEnabled()) {
            // 使用占位符方式记录日志，避免字符串拼接的性能开销
            // {} 是SLF4J的占位符，运行时会被后续参数替换
            logger.info("[{}] {}, Client IP: {}",
                    request.getMethod(),
                    request.getRequestURI(),
                    getClientIp(request));
        }

        // 返回true表示继续流程，返回false表示中断流程
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {

        Long startTime = startTimeThreadLocal.get();
        if (startTime != null) {
            // 计算该请求花费的时间
            long costTime = System.currentTimeMillis() - startTime;

            // 记录请求完成信息
            logger.info("[{}] {} | Status: {} | Cost: {}ms",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    costTime);

            // 如果发生异常，记录错误信息
            if (ex != null) {
                logger.error("请求异常: " + ex.getMessage(), ex);
            }

            // 清理ThreadLocal，防止内存泄漏
            startTimeThreadLocal.remove();
            // 清理MDC中的请求ID
            MDC.remove("traceId");

        }
    }

    /**
     * 获取客户端真实IP地址
     * @param request HttpServletRequest对象
     * @return 客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        // 尝试从各种可能的请求头中获取客户端真实IP
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}