package com.example.smartagent.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class WeatherTool implements ToolExecutor.Tool {

    @Override
    public String getName() {
        return "WeatherTool";
    }

    @Override
    public String getDescription() {
        return "查询指定城市的天气信息";
    }

    @Override
    public ToolExecutor.ToolResult execute(Map<String, Object> params) {
        String city = (String) params.get("city");
        log.info("查询天气: {}", city);
        
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String weatherInfo = String.format("城市: %s, 天气: 晴, 温度: 25°C", city);
        return ToolExecutor.ToolResult.success(weatherInfo);
    }

    @Override
    public boolean validateParams(Map<String, Object> params) {
        return params.containsKey("city") && params.get("city") instanceof String;
    }
}
