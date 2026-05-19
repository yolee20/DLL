package com.example.smartagent.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 完全基于Spring AI的工具类
 * 使用@Tool注解自动注册为模型可调用工具
 */
@Component
public class SpringAITools {

    private static final Map<String, String> WEATHER_DATA = new HashMap<>();
    private static final Random RANDOM = new Random();

    static {
        WEATHER_DATA.put("北京", "晴天，22-28°C，微风");
        WEATHER_DATA.put("上海", "多云，20-26°C，东南风3级");
        WEATHER_DATA.put("广州", "阵雨，25-32°C，湿度80%");
        WEATHER_DATA.put("深圳", "阴天，24-30°C，东北风2级");
        WEATHER_DATA.put("杭州", "晴天，18-24°C，微风");
    }

    /**
     * 获取当前时间
     */
    @Tool(name = "get_current_time", description = "获取当前系统时间")
    public String getCurrentTime() {
        return "当前时间: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * 查询天气
     */
    @Tool(name = "get_weather", description = "查询指定城市的天气信息")
    public String getWeather(@ToolParam(description = "城市名称") String city) {
        if (city == null || city.trim().isEmpty()) {
            return "请提供有效的城市名称";
        }
        String weather = WEATHER_DATA.get(city);
        if (weather == null) {
            return String.format("%s的天气：%s，%d-%d°C", city, 
                    RANDOM.nextBoolean() ? "晴天" : "多云", 
                    15 + RANDOM.nextInt(15), 
                    25 + RANDOM.nextInt(10));
        }
        return city + "的天气：" + weather;
    }

    /**
     * 计算数学表达式（简单版本）
     */
    @Tool(name = "calculate", description = "计算简单的数学表达式，如'1+1'、'5*3'")
    public String calculate(@ToolParam(description = "数学表达式") String expression) {
        try {
            return String.valueOf(evalSimpleExpression(expression));
        } catch (Exception e) {
            return "计算失败: " + e.getMessage();
        }
    }

    /**
     * 简单计算器
     */
    private double evalSimpleExpression(String expr) {
        expr = expr.replaceAll("\\s+", "");
        if (expr.contains("+")) {
            String[] parts = expr.split("\\+", 2);
            return Double.parseDouble(parts[0]) + Double.parseDouble(parts[1]);
        } else if (expr.contains("-")) {
            String[] parts = expr.split("-", 2);
            return Double.parseDouble(parts[0]) - Double.parseDouble(parts[1]);
        } else if (expr.contains("*")) {
            String[] parts = expr.split("\\*", 2);
            return Double.parseDouble(parts[0]) * Double.parseDouble(parts[1]);
        } else if (expr.contains("/")) {
            String[] parts = expr.split("/", 2);
            return Double.parseDouble(parts[0]) / Double.parseDouble(parts[1]);
        }
        return Double.parseDouble(expr);
    }
}
