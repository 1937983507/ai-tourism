package com.example.aitourism.ai.tool;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.example.aitourism.mapper.POIMapper;
import com.example.aitourism.entity.POI;
import java.util.List;


// 基于城市名获取该城市的景点信息
@Component
@Slf4j
public class POISearchTool  extends BaseTool {

    @Autowired
    private POIMapper poiMapper;

    public POISearchTool() {
        // 无需super调用
    }

    @Override
    public String getName() {
        return "poiSearch";
    }

    @Override
    public String getDescription() {
        return "根据城市名获取景点信息";
    }

    @Tool("根据城市名获取景点信息")
    public String poiSearch(
            @P("城市名称，例如: 北京、上海、西安（不要加后缀）") String cityName,
            @P("要返回的景点数量，例如: 10") Integer poiCount
    ) {

        log.info("开始调用poiSearch工具，城市名称: {}, 景点数量: {}", cityName, poiCount);

        try {
            // 参数校验
            if (StrUtil.isBlank(cityName)) {
                log.warn("城市名称为空");
                return JSONUtil.toJsonStr(new JSONObject().set("error", "城市名称不能为空"));
            }

            // 设置默认数量
            if (poiCount == null || poiCount <= 0) {
                poiCount = 10;
            }

            // 查询数据库
            List<POI> poiList = poiMapper.findByCityName(cityName, poiCount);

            if (poiList == null || poiList.isEmpty()) {
                log.info("未找到城市 {} 的景点信息", cityName);
                return JSONUtil.toJsonStr(new JSONObject()
                        .set("cityName", cityName)
                        .set("count", 0)
                        .set("pois", new JSONArray()));
            }

            // 构建JSON数组
            JSONArray jsonArray = new JSONArray();
            for (POI poi : poiList) {
                JSONObject poiJson = new JSONObject();
                poiJson.set("id", poi.getId());
                poiJson.set("poiName", poi.getPoiName());
                poiJson.set("cityName", poi.getCityName());
                poiJson.set("poiDescription", poi.getPoiDescription());
                poiJson.set("poiLongitude", poi.getPoiLongitude());
                poiJson.set("poiLatitude", poi.getPoiLatitude());
                poiJson.set("poiRankInCity", poi.getPoiRankInCity());
                poiJson.set("poiRankInChina", poi.getPoiRankInChina());
                jsonArray.add(poiJson);
            }

            // 构建返回结果
            JSONObject result = new JSONObject();
            result.set("cityName", cityName);
            result.set("count", poiList.size());
            result.set("pois", jsonArray);

            String jsonStr = JSONUtil.toJsonStr(result);
            log.info("成功查询到 {} 个景点信息：{}", poiList.size(), jsonStr);
            return jsonStr;
        } catch (Exception e) {
            log.error("查询景点信息时发生错误: {}", e.getMessage(), e);
            return JSONUtil.toJsonStr(new JSONObject().set("error", "查询景点信息失败: " + e.getMessage()));
        }
    }
}
