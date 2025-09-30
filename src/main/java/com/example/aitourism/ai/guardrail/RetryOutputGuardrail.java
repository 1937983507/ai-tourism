package com.example.aitourism.ai.guardrail;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrailResult;

/**
 * 重试输出护轨，用于检测大模型输出内容的有效性和安全性。
 */
public class RetryOutputGuardrail implements OutputGuardrail {

    /**
     * 校验大模型返回的 AI 消息内容。
     * @param responseFromLLM AI 消息对象
     * @return OutputGuardrailResult 校验结果
     */
    @Override
    public OutputGuardrailResult validate(AiMessage responseFromLLM) {
        String aiResponse = responseFromLLM.text();
        // 检查响应是否为空或全是空白
        if (aiResponse == null || aiResponse.trim().isEmpty()) {
            return reprompt("响应内容为空", "请重新生成完整的内容");
        }
        // 检查响应内容是否过短
        if (aiResponse.trim().length() < 10) {
            return reprompt("响应内容过短", "请提供更详细的内容");
        }
        // 检查是否包含敏感信息或不当内容
        if (containsSensitiveContent(aiResponse)) {
            return reprompt("包含敏感信息", "请重新生成内容，避免包含敏感信息");
        }
        // 校验通过
        return success();
    }

    /**
     * 检查内容中是否包含敏感词。
     * @param response AI 输出内容
     * @return boolean 是否包含敏感内容
     */
    private boolean containsSensitiveContent(String response) {
        String lowerResponse = response.toLowerCase();
        // 敏感词列表
        String[] sensitiveWords = {
                "密码", "password", "secret", "token",
                "api key", "私钥", "证书", "credential"
        };
        for (String word : sensitiveWords) {
            if (lowerResponse.contains(word)) {
                return true;
            }
        }
        return false;
    }
}