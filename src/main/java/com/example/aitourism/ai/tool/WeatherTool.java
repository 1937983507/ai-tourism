package com.example.aitourism.ai.tool;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.text.SimpleDateFormat;
import java.util.*;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.example.aitourism.service.AbTestService;
import com.example.aitourism.monitor.MonitorContextHolder;
import com.example.aitourism.monitor.MonitorContext;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.Duration;
import java.time.Instant;

@Component
@Slf4j
public class WeatherTool extends BaseTool {
    
    // 地理编码地址
    private static final String ENCODE_API_URL = "http://api.openweathermap.org/geo/1.0/direct";
    // 获取天气预报地址,，Open-Meteo API 地址
    private static final String OPEN_METEO_API_URL = "https://api.open-meteo.com/v1/forecast";

    @Value("${openweather.api-key}")
    private String openWeatherApiKey;
    
    @Autowired
    private AbTestService abTestService;
    

    // 使用 Caffeine 做 5 分钟本地缓存，防止同参数重复外调
    private static final Cache<String, String> weatherCache = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(1024)
            .build();

    public WeatherTool() {
        // 无需super调用
    }

    @Override
    public String getName() {
        return "weatherForecast";
    }

    @Override
    public String getDescription() {
        return "获取指定城市的逐天天气预报，支持1-16天的预报";
    }

    @Tool("根据城市名获取未来若干天的逐天天气预报，天数范围1-16")
    public String weatherForecast(
            @P("城市名称，例如: 北京 / Shanghai / New York") String cityName,
            @P("要返回的预测天数，范围1-16") Integer dayCount
    ) {

        // log.info("开始调用weatherForecast工具，城市名称: {}, 天数: {}", cityName, dayCount);

        // 获取监控上下文
        MonitorContext context = MonitorContextHolder.getContext();
        String userId = context != null ? context.getUserId() : "unknown";
        String sessionId = context != null ? context.getSessionId() : "unknown";
        
        // 记录工具调用开始时间
        Instant startTime = Instant.now();

        // TODO 逻辑需要在完善一下
        // 应该先判断是否要用缓存，若是要用缓存才进行缓存判断，若是不用缓存，则直接调用外部API进行获取数据
        
        // 开始检查是否命中
        String cacheKey = cityName + "|" + dayCount;
        String cached = weatherCache.getIfPresent(cacheKey);
        boolean fromCache = false;
        
        // 检查是否应该使用缓存（A/B测试）
        boolean shouldUseCache = abTestService.shouldUseToolCache(userId, sessionId);
        
        if (shouldUseCache && cached != null) {
            // 命中缓存 且 允许缓存的情况，记录时间
            log.info("命中天气缓存，城市名称: {}, 天数: {}", cityName, dayCount);
            fromCache = true;
            
            // 记录缓存命中性能数据
            Duration responseTime = Duration.between(startTime, Instant.now());
            abTestService.recordToolPerformance(userId, sessionId, "weatherForecast", responseTime, true);

            // 直接返回了缓存结果
            return cached;
        }
        
        // 没有命中缓存 or 不允许缓存的情况，则调用外部API进行获取数据

        if (dayCount == null) {
            dayCount = 7;
        }
        
        // 参数验证
        if (StrUtil.isBlank(cityName)) {
            return "城市名称不能为空";
        }
        if(dayCount < 1) dayCount=1;
        if(dayCount > 16) dayCount=16;

        try {

            double[] cityLatLon = encode(cityName);  // 获取经纬度
            if(cityLatLon==null){
                return "获取经纬度报错。";
            }

            // 计算开始与结束日期
            LocalDate today = LocalDate.now();
            LocalDate endDate = today.plusDays(dayCount - 1);
            String startDate = today.toString();
            String endDateStr = endDate.toString();

            // 构建 API URL
            String url = String.format(
                    "%s?latitude=%s&longitude=%s&start_date=%s&end_date=%s&daily=temperature_2m_min,temperature_2m_max,temperature_2m_mean,precipitation_sum,snowfall_sum,windspeed_10m_max,windgusts_10m_max,winddirection_10m_dominant&timezone=auto",
                    OPEN_METEO_API_URL, cityLatLon[0], cityLatLon[1], startDate, endDateStr
            );
//            System.out.println("请求URL：" + url);

            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(url)
                    .build();

            // 发送HTTP请求
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("Open-Meteo API 调用失败："+response);
                    return "暂时无法获取天气数据，请忽略此错误";
                }

                // 解析 JSON
                String jsonStr = response.body().string();
                JSONObject result = JSONUtil.parseObj(jsonStr);
                JSONObject daily = result.getJSONObject("daily");

                JSONArray times = daily.getJSONArray("time");
                JSONArray tMin = daily.getJSONArray("temperature_2m_min");
                JSONArray tMax = daily.getJSONArray("temperature_2m_max");
                JSONArray tMean = daily.getJSONArray("temperature_2m_mean");
                JSONArray precip = daily.getJSONArray("precipitation_sum");
                JSONArray snow = daily.getJSONArray("snowfall_sum");
                JSONArray windMax = daily.getJSONArray("windspeed_10m_max");
                JSONArray gustMax = daily.getJSONArray("windgusts_10m_max");
                JSONArray windDir = daily.getJSONArray("winddirection_10m_dominant");

                JSONArray resultArray = new JSONArray();

                for (int i = 0; i < times.size(); i++) {
                    JSONObject dayWeather = new JSONObject();
                    dayWeather.put("日期", times.getStr(i));
                    dayWeather.put("最低温(℃)", String.valueOf(tMin.getDouble(i)));
                    dayWeather.put("最高温(℃)", String.valueOf(tMax.getDouble(i)));
                    dayWeather.put("平均温(℃)", String.valueOf(tMean.getDouble(i)));
                    dayWeather.put("降水量(mm)", String.valueOf(precip.getDouble(i)));
                    dayWeather.put("降雪量(cm)", String.valueOf(snow.getDouble(i)));
                    dayWeather.put("最大风速(m/s)", String.valueOf(windMax.getDouble(i)));
                    dayWeather.put("最大阵风(m/s)", String.valueOf(gustMax.getDouble(i)));
                    dayWeather.put("主导风向(°)", String.valueOf(windDir.getDouble(i)));

                    resultArray.add(dayWeather);
                }

                String resultText = resultArray.toString();
                log.info("获取天气数据成功: {}", resultText);
                
                // 只有在使用缓存时才存储到缓存中
                if (shouldUseCache) {
                    weatherCache.put(cacheKey, resultText);
                }
                
                // 记录缓存未命中性能数据
                Duration responseTime = Duration.between(startTime, Instant.now());
                abTestService.recordToolPerformance(userId, sessionId, "weatherForecast", responseTime, false);
                
                return resultText;
            }

        } catch (Exception e) {
            log.error("获取天气数据时发生异常: {}", e.getMessage(), e);
            return "获取天气数据时发生错误，请忽略此错误";
        }
    }

    // 无状态工具，无需额外结构



    // 地理编码，通过城市名获取经纬度
    private double[] encode(String cityName){
        // 构建API请求URL
        String url = String.format("%s?q=%s&limit=1&appid=%s",
            ENCODE_API_URL, cityName, openWeatherApiKey);

//        System.out.println("地理编码URL："+url);
        // 发送HTTP请求
        try (HttpResponse response = HttpRequest.get(url).execute()) {
            if (!response.isOk()) {
                log.error("OpenWeatherMap API调用失败，状态码: {}", response.getStatus());
                return null;
            }

            JSONArray jsonArray = new JSONArray(response.body());

            // 获取第0个元素（JSON对象）
            JSONObject cityData = jsonArray.getJSONObject(0);

            // 提取经纬度
            double lat = cityData.getDouble("lat");
            double lon = cityData.getDouble("lon");

            // 返回经纬度数组，第一个元素是纬度，第二个是经度
            return new double[]{lat, lon};

        }catch (Exception e) {
            log.error("获取城市经纬度时发生异常: {}", e.getMessage(), e);
            return null;
        }

    }

    // 将Unix时间戳转换为可读日期时间
    private static String formatUnixTime(long unixTime) {
        try {
            Date date = new Date(unixTime * 1000);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return sdf.format(date);
        } catch (Exception e) {
            return "时间格式错误";
        }
    }

}
