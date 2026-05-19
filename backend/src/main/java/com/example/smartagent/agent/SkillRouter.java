
package com.example.smartagent.agent;

import com.example.smartagent.skill.QASkill;
import com.example.smartagent.skill.OrderSkill;
import com.example.smartagent.skill.Skill;
import com.example.smartagent.skill.WeatherSkill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 技能路由器（SkillRouter）
 * <p>
 * 负责根据用户意图路由到相应的技能模块。
 * 主要功能：
 * 1. 维护意图-技能映射关系
 * 2. 根据意图找到对应的技能
 * 3. 如果意图不明确，返回默认技能
 * </p>
 */
@Component
@Slf4j
public class SkillRouter {

    /** 意图到技能的映射 */
    private final Map<String, Skill> intentSkillMap = new HashMap<>();

    /** 默认技能 - 当无法匹配其他技能时使用 */
    private final Skill defaultSkill;

    /**
     * 构造函数，初始化意图-技能映射
     *
     * @param qaSkill      问答技能
     * @param orderSkill   订单技能
     * @param weatherSkill 天气技能
     */
    public SkillRouter(QASkill qaSkill, OrderSkill orderSkill, WeatherSkill weatherSkill) {
        // 注册问答技能支持的意图
        for (String intent : qaSkill.getSupportedIntents()) {
            intentSkillMap.put(intent, qaSkill);
        }
        
        // 注册订单技能支持的意图
        for (String intent : orderSkill.getSupportedIntents()) {
            intentSkillMap.put(intent, orderSkill);
        }
        
        // 注册天气技能支持的意图
        for (String intent : weatherSkill.getSupportedIntents()) {
            intentSkillMap.put(intent, weatherSkill);
        }
        
        // 设置默认技能为问答技能
        this.defaultSkill = qaSkill;
        
        log.info("SkillRouter initialized with {} intent-skill mappings", intentSkillMap.size());
    }

    /**
     * 根据意图路由到对应的技能
     *
     * @param intent 意图类型
     * @return 对应的技能实例
     */
    public Skill route(String intent) {
        Skill skill = intentSkillMap.get(intent);
        
        if (skill == null) {
            log.warn("No skill found for intent: {}, using default skill", intent);
            skill = defaultSkill;
        }
        
        return skill;
    }

    /**
     * 获取意图-技能映射
     *
     * @return 映射关系
     */
    public Map<String, Skill> getIntentSkillMap() {
        return intentSkillMap;
    }
}
