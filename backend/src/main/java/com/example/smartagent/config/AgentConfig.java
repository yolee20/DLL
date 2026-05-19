
package com.example.smartagent.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AgentConfig {
    @Value("${agent.intent-threshold:0.7}")
    private double intentThreshold;
    
    @Value("${agent.max-history-length:10}")
    private int maxHistoryLength;
    
    @Value("${agent.default-skill:QASkill}")
    private String defaultSkill;
    
    public double getIntentThreshold() {
        return intentThreshold;
    }
    
    public int getMaxHistoryLength() {
        return maxHistoryLength;
    }
    
    public String getDefaultSkill() {
        return defaultSkill;
    }
}
