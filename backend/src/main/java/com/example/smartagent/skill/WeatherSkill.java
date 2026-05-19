package com.example.smartagent.skill;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component("weatherSkill")
@Slf4j
@SkillDescriptor(
        name = "WeatherSkill",
        description = "天气查询技能",
        category = "tool",
        version = "1.0.0"
)
public class WeatherSkill extends AbstractSkill {

    private final Map<String, Map<String, String>> mockWeather = new HashMap<>();

    public WeatherSkill() {
        Map<String, String> beijing = new HashMap<>();
        beijing.put("temp", "26°C");
        beijing.put("condition", "晴转多云");
        beijing.put("wind", "东北风3级");
        beijing.put("humidity", "45%");
        mockWeather.put("北京", beijing);

        Map<String, String> shanghai = new HashMap<>();
        shanghai.put("temp", "28°C");
        shanghai.put("condition", "多云");
        shanghai.put("wind", "东南风2级");
        shanghai.put("humidity", "65%");
        mockWeather.put("上海", shanghai);

        Map<String, String> guangzhou = new HashMap<>();
        guangzhou.put("temp", "32°C");
        guangzhou.put("condition", "雷阵雨");
        guangzhou.put("wind", "西南风4级");
        guangzhou.put("humidity", "85%");
        mockWeather.put("广州", guangzhou);
    }

    @Override
    public String getName() {
        return "WeatherSkill";
    }

    @Override
    protected SkillResult doExecute(String userMessage, Map<String, Object> context) {
        String city = extractCity(userMessage);

        if (city == null) {
            city = (String) context.get("city");
        }

        if (city != null) {
            Map<String, String> weather = mockWeather.get(city);
            if (weather != null) {
                String response = buildWeatherResponse(city, weather);
                return success(response, 0.88);
            } else {
                return failure("暂不支持查询城市: " + city);
            }
        }

        return failure("未识别到城市名称，请告诉我要查询哪个城市的天气");
    }

    @Override
    protected Set<String> getDefaultSupportedIntents() {
        return Set.of("weather", "天气", "温度", "预报");
    }

    private String extractCity(String message) {
        for (String city : mockWeather.keySet()) {
            if (message.contains(city)) {
                return city;
            }
        }
        return null;
    }

    private String buildWeatherResponse(String city, Map<String, String> weather) {
        return String.format("%s今日天气：%s，温度%s，%s，湿度%s",
                city,
                weather.get("condition"),
                weather.get("temp"),
                weather.get("wind"),
                weather.get("humidity"));
    }
}