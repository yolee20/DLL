package com.example.smartagent.memory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    private String role;

    private String content;

    private Long timestamp;

    public static Message user(String content) {
        return Message.builder()
                .role("user")
                .content(content)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static Message assistant(String content) {
        return Message.builder()
                .role("assistant")
                .content(content)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
