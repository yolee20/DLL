
package com.example.smartagent.skill;

import java.util.Map;
import java.util.Set;

/**
 * 技能接口（Skill）
 * <p>
 * 定义技能的基本接口，所有技能实现类都必须实现此接口。
 * 技能是智能代理系统中的功能模块，负责处理特定类型的用户请求。
 * </p>
 */
public interface Skill {

    /**
     * 获取技能名称
     *
     * @return 技能名称
     */
    String getName();

    /**
     * 执行技能
     * <p>
     * 根据用户消息和上下文执行技能逻辑，返回执行结果。
     * </p>
     *
     * @param userMessage 用户输入的消息
     * @param context     对话上下文
     * @return 技能执行结果
     */
    SkillResult execute(String userMessage, Map<String, Object> context);

    /**
     * 获取技能支持的意图类型集合
     * <p>
     * 当用户意图匹配到集合中的任意一个意图时，技能路由器会将请求路由到该技能。
     * </p>
     *
     * @return 支持的意图类型集合
     */
    Set<String> getSupportedIntents();
}
