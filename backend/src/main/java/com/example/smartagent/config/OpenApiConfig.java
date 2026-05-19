
package com.example.smartagent.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        Info info = new Info()
                .title("SmartAgent API")
                .version("1.0.0")
                .description("SmartAgent 智能代理系统 API 文档")
                .contact(new Contact()
                        .name("SmartAgent Team")
                        .email("support@smartagent.com"));

        return new OpenAPI().info(info);
    }
}
