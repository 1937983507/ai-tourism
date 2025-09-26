package com.example.aitourism.util;

import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;

/**
 * MCP工具结果裁剪工具类
 * 用于限制MCP工具返回结果的长度，避免超出LLM的token限制
 * 免费API对模型输入有4096 token上限，所以需要对MCP工具返回结果进行裁剪
 */
@Slf4j
public class McpResultTruncator {
    
    // 用于检测中文字符的正则表达式
    private static final Pattern CHINESE_PATTERN = Pattern.compile("[\\u4e00-\\u9fa5]");
    
    // 用于检测自然断点的正则表达式（句号、问号、感叹号、换行符等）
    private static final Pattern NATURAL_BREAK_PATTERN = Pattern.compile("[。！？\\.\\!\\?]");
    
    /**
     * 智能裁剪MCP工具返回的结果
     * @param result 原始结果
     * @param maxLength 最大长度限制
     * @return 裁剪后的结果
     */
    public static String truncateResult(String result, int maxLength) {
        if (result == null || result.length() <= maxLength) {
            return result;
        }
        
        log.info("开始裁剪结果，原始长度: {} 字符，限制长度: {} 字符", result.length(), maxLength);
        
        // 确保maxLength至少为50，避免计算错误
        if (maxLength < 50) {
            maxLength = 50;
        }
        
        // 计算实际可用的裁剪长度（预留裁剪提示的空间）
        int availableLength = maxLength - 50; // 预留50个字符用于添加裁剪提示
        
        // 确保availableLength为正数
        if (availableLength <= 0) {
            availableLength = maxLength / 2; // 如果maxLength太小，使用一半长度
        }
        
        // 获取需要裁剪的文本部分
        String textToTruncate = result.substring(0, Math.min(availableLength, result.length()));
        
        // 尝试在自然断点处截断
        int lastNaturalBreak = findLastNaturalBreak(textToTruncate);
        
        String truncatedText;
        if (lastNaturalBreak > availableLength * 0.7) {
            // 如果找到了合适的自然断点（在70%位置之后），使用该断点
            truncatedText = textToTruncate.substring(0, lastNaturalBreak);
            log.debug("在自然断点处截断，位置: {}", lastNaturalBreak);
        } else {
            // 否则直接截断到指定长度
            truncatedText = textToTruncate;
            log.debug("直接截断到指定长度: {}", availableLength);
        }
        
        // 添加裁剪提示
        String truncationSuffix = generateTruncationSuffix(result.length(), maxLength);
        String finalResult = truncatedText + truncationSuffix;
        
        log.info("裁剪完成: {} -> {} 字符", result.length(), finalResult.length());
        
        return finalResult;
    }
    
    /**
     * 查找最后一个自然断点位置
     * @param text 文本
     * @return 最后一个自然断点的位置，如果没找到返回-1
     */
    private static int findLastNaturalBreak(String text) {
        // 按优先级查找断点：句号 > 问号 > 感叹号 > 换行符
        int lastPeriod = text.lastIndexOf('。');
        int lastQuestion = text.lastIndexOf('？');
        int lastExclamation = text.lastIndexOf('！');
        int lastNewline = text.lastIndexOf('\n');
        
        // 返回最靠后的有效断点
        int maxBreak = Math.max(Math.max(lastPeriod, lastQuestion), Math.max(lastExclamation, lastNewline));
        
        // 确保断点位置合理（不能太靠前）
        if (maxBreak > text.length() * 0.3) {
            return maxBreak + 1; // 包含断点字符
        }
        
        return -1; // 没有找到合适的断点
    }
    
    /**
     * 生成裁剪提示后缀
     * @param originalLength 原始长度
     * @param maxLength 最大长度
     * @return 裁剪提示文本
     */
    private static String generateTruncationSuffix(int originalLength, int maxLength) {
        int truncatedChars = originalLength - maxLength + 50; // 50是预留的提示空间
        
        // 根据裁剪的字符数量生成不同的提示
        if (truncatedChars > 1000) {
            return String.format("\n\n[已裁剪约%d个字符，保留核心信息]", truncatedChars);
        } else if (truncatedChars > 500) {
            return String.format("\n\n[已裁剪%d个字符，保留主要内容]", truncatedChars);
        } else {
            return "\n\n[已裁剪部分内容]";
        }
    }
    
    /**
     * 估算文本的token数量（粗略估算）
     * 中文字符按1个token计算，英文单词按平均长度估算
     * @param text 文本
     * @return 估算的token数量
     */
    public static int estimateTokenCount(String text) {
        if (text == null) return 0;
        
        int chineseChars = 0;
        int englishWords = 0;
        
        // 统计中文字符
        for (char c : text.toCharArray()) {
            if (CHINESE_PATTERN.matcher(String.valueOf(c)).matches()) {
                chineseChars++;
            }
        }
        
        // 统计英文单词（简单按空格分割）
        String[] words = text.replaceAll("[\\u4e00-\\u9fa5]", "").split("\\s+");
        for (String word : words) {
            if (!word.trim().isEmpty()) {
                englishWords++;
            }
        }
        
        // 粗略估算：中文字符1个token，英文单词平均1.3个token
        return chineseChars + (int)(englishWords * 1.3);
    }
    
    /**
     * 检查文本是否需要裁剪
     * @param text 文本
     * @param maxLength 最大长度
     * @return 是否需要裁剪
     */
    public static boolean needsTruncation(String text, int maxLength) {
        return text != null && text.length() > maxLength;
    }
}
