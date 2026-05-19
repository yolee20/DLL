package com.example.smartagent.skill;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component("orderSkill")
@Slf4j
@SkillDescriptor(
        name = "OrderSkill",
        description = "订单查询和物流跟踪技能",
        category = "tool",
        version = "1.0.0"
)
public class OrderSkill extends AbstractSkill {

    private final Map<String, Map<String, String>> mockOrders = new HashMap<>();

    public OrderSkill() {
        Map<String, String> order1 = new HashMap<>();
        order1.put("status", "已发货");
        order1.put("logistics", "顺丰速运 SF1234567890");
        order1.put("tracking", "【北京】已发货 -> 【上海】运输中 -> 【杭州】派送中");
        order1.put("product", "智能音箱 Pro");
        mockOrders.put("ORD20240101001", order1);

        Map<String, String> order2 = new HashMap<>();
        order2.put("status", "待发货");
        order2.put("logistics", "待分配");
        order2.put("tracking", "订单已确认，等待发货");
        order2.put("product", "无线耳机");
        mockOrders.put("ORD20240102002", order2);

        Map<String, String> order3 = new HashMap<>();
        order3.put("status", "已完成");
        order3.put("logistics", "京东物流 JD9876543210");
        order3.put("tracking", "【深圳】已签收");
        order3.put("product", "平板电脑");
        mockOrders.put("ORD20240103003", order3);
    }

    @Override
    public String getName() {
        return "OrderSkill";
    }

    @Override
    protected SkillResult doExecute(String userMessage, Map<String, Object> context) {
        String orderNumber = extractOrderNumber(userMessage);

        if (orderNumber == null) {
            orderNumber = (String) context.get("orderNumber");
        }

        if (orderNumber != null) {
            Map<String, String> order = mockOrders.get(orderNumber);
            if (order != null) {
                String response = buildOrderResponse(orderNumber, order);
                return success(response, 0.9);
            } else {
                return failure("未找到订单号: " + orderNumber);
            }
        }

        return failure("未识别到订单号，请提供订单号以便查询");
    }

    @Override
    protected Set<String> getDefaultSupportedIntents() {
        return Set.of("order", "订单", "物流", "发货", "快递");
    }

    private String extractOrderNumber(String message) {
        for (String word : message.split("\\s+")) {
            if (word.toUpperCase().startsWith("ORD")) {
                return word.toUpperCase();
            }
        }
        return null;
    }

    private String buildOrderResponse(String orderNumber, Map<String, String> order) {
        return String.format("订单 %s 状态：%s\n商品：%s\n物流：%s\n跟踪信息：%s",
                orderNumber,
                order.get("status"),
                order.get("product"),
                order.get("logistics"),
                order.get("tracking"));
    }
}