package com.example.aitourism.ai.tool;

import java.util.Map;


/**
 * 工具基类
 * 定义所有工具的通用接口
 */
public abstract class BaseTool {

    // 工具名称
    public abstract String getName();

    // 工具描述
    public String getDescription() {
        return "";
    }

    // 工具统一执行入口
    public String execute(Map<String, Object> input) {
        throw new UnsupportedOperationException("未实现execute方法");
    }
}
