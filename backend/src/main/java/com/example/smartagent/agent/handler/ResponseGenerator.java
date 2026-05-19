package com.example.smartagent.agent.handler;

import com.example.smartagent.skill.SkillResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ResponseGenerator {

    public String generateResponse(SkillResult skillResult) {
        if (skillResult.getSuccess()) {
            return generateSuccessResponse(skillResult);
        } else {
            return generateFailureResponse(skillResult);
        }
    }

    private String generateSuccessResponse(SkillResult skillResult) {
        String answer = skillResult.getAnswer();
        
        if (answer != null && !answer.isEmpty()) {
            return answer;
        }
        
        return "已完成操作，如有其他问题请继续提问。";
    }

    private String generateFailureResponse(SkillResult skillResult) {
        String errorMessage = skillResult.getAnswer();
        
        if (errorMessage != null && !errorMessage.isEmpty()) {
            return "抱歉，" + errorMessage;
        }
        
        return "抱歉，我暂时无法处理您的请求，请稍后再试或联系人工客服。";
    }
}
