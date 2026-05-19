
package com.example.smartagent.agent;

import com.example.smartagent.mcp.ModelClient;
import com.example.smartagent.mcp.ModelRequest;
import com.example.smartagent.mcp.ModelResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 意图解析器（IntentParser）
 * <p>
 * 负责解析用户输入的意图，将自然语言转换为系统可识别的意图类型。
 * 主要功能：
 * 1. 调用意图识别模型进行意图分类
 * 2. 根据置信度阈值判断是否需要追问
 * 3. 返回意图识别结果
 * </p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IntentParser {

    /** 模型客户端 - 用于调用意图识别模型 */
    private final ModelClient modelClient;

    /** 意图识别置信度阈值，低于此值认为意图不明确 */
    @Value("${agent.intent-threshold:0.7}")
    private double intentThreshold;

    /**
     * 解析用户意图
     * <p>
     * 调用意图识别模型，返回意图类型和置信度
     * </p>
     *
     * @param userMessage 用户输入的消息
     * @param context     对话上下文
     * @return 意图识别结果
     */
    public IntentResult parse(String userMessage, Map<String, Object> context) {
        log.info("Parsing intent for message: {}", userMessage);

        // 调用意图识别模型
        ModelResponse response = modelClient.callModel("intent_model",
                ModelRequest.of(userMessage));

        String intent = response.getOutput();
        double confidence = response.getConfidence();

        // 如果置信度低于阈值，标记为不确定意图
        if (confidence < intentThreshold) {
            intent = "unknown";
        }

        log.info("Intent parsed: {} with confidence: {}", intent, confidence);
        return new IntentResult(intent, confidence);
    }

    /**
     * 意图识别结果记录
     *
     * @param intent     意图类型（如：qa, order, weather, unknown）
     * @param confidence 置信度（0.0-1.0）
     */
    public record IntentResult(String intent, double confidence) {}
}
