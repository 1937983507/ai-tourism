package com.example.aitourism.ai.tool;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具管理器
 * 统一管理所有工具，提供根据名称获取工具以及全部工具列表
 */
@Component
@Slf4j
public class ToolManager {
    
    // 自动注入所有工具
    @Resource
    private BaseTool[] tools;
    // 工具名称到工具实例的映射
    private final Map<String, BaseTool> toolMap = new HashMap<>();

    @PostConstruct
    public void initTools() {
        for (BaseTool tool : tools) {
            toolMap.put(tool.getName(), tool);
            log.info("注册工具: {} -> {}", tool.getName(), tool.getDescription());
        }
        log.info("工具管理器初始化完成，共注册 {} 个工具", toolMap.size());
    }

    public BaseTool getTool(String toolName) {
        return toolMap.get(toolName);
    }

    public BaseTool[] getAllTools() {
        return tools;
    }
}

