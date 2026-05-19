
package com.example.smartagent.agent;

import com.example.smartagent.skill.SkillResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 响应生成器（ResponseGenerator）
 * <p>
 * 负责根据技能执行结果生成最终的自然语言响应。
 * 主要功能：
 * 1. 将技能结果转换为友好的自然语言响应
 * 2. 处理成功和失败的不同情况
 * 3. 添加必要的引导信息
 * </p>
 */
@Component
@Slf4j
public class ResponseGenerator {

    /**
     * 根据技能结果生成响应
     *
     * @param skillResult 技能执行结果
     * @return 最终的自然语言响应
     */
    public String generateResponse(SkillResult skillResult) {
        if (skillResult.getSuccess()) {
            return generateSuccessResponse(skillResult);
        } else {
            return generateFailureResponse(skillResult);
        }
    }

    /**
     * 生成成功响应
     *
     * @param skillResult 成功的技能结果
     * @return 友好的成功响应
     */
    private String generateSuccessResponse(SkillResult skillResult) {
        String answer = skillResult.getAnswer();
        
        // 如果回答已经是完整的响应，直接返回
        if (answer != null && !answer.isEmpty()) {
            return answer;
        }
        
        // 默认成功响应
        return "已完成操作，如有其他问题请继续提问。";
    }

    /**
     * 生成失败响应
     *
     * @param skillResult 失败的技能结果
     * @return 友好的失败响应
     */
    private String generateFailureResponse(SkillResult skillResult) {
        String errorMessage = skillResult.getAnswer();
        
        if (errorMessage != null && !errorMessage.isEmpty()) {
            return "抱歉，" + errorMessage;
        }
        
        // 默认失败响应
        return "抱歉，我暂时无法处理您的请求，请稍后再试或联系人工客服。";
    }
}
