package com.example.smartagent.config;

import io.milvus.client.MilvusClient;
import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class MilvusConfig {

    @Value("${milvus.host:localhost}")
    private String host;

    @Value("${milvus.port:19530}")
    private int port;

    @Bean
    public MilvusClient milvusClient() {
        try {
            // ✅ 在构造函数中传入连接参数
            ConnectParam connectParam = ConnectParam.newBuilder()
                    .withHost(host)
                    .withPort(port)
                    .build();

            MilvusClient client = new MilvusServiceClient(connectParam);
            log.info("Successfully connected to Milvus at {}:{}", host, port);
            return client;
        } catch (Exception e) {
            log.error("Failed to create Milvus client: {}", e.getMessage(), e);
            // 返回 null，业务层需要判断 milvusAvailable
            return null;
        }
    }
}