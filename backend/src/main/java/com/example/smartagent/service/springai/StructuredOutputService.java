package com.example.smartagent.service.springai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.StreamingChatModel;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class StructuredOutputService {

    private final ChatModel chatModel;
    private final StreamingChatModel streamingChatModel;

    public <T> T generateStructured(String prompt, Class<T> targetClass) {
        try {
            BeanOutputConverter<T> converter = new BeanOutputConverter<>(targetClass);
            String format = converter.getFormat();
            
            String fullPrompt = prompt + "\n\n" + format;
            
            ChatClient chatClient = ChatClient.builder(chatModel).build();
            String response = chatClient.prompt()
                    .user(fullPrompt)
                    .call()
                    .content();
            
            return converter.convert(response);
        } catch (Exception e) {
            log.error("Failed to generate structured output for class: {}", targetClass.getSimpleName(), e);
            throw new RuntimeException("结构化输出生成失败", e);
        }
    }

    public List<Map<String, Object>> generateList(String prompt) {
        try {
            String format = "请返回一个JSON数组，数组中的每个元素是一个包含键值对的对象。格式：[{\"key1\":\"value1\",\"key2\":\"value2\"},...]";
            
            String fullPrompt = prompt + "\n\n" + format;
            
            ChatClient chatClient = ChatClient.builder(chatModel).build();
            String response = chatClient.prompt()
                    .user(fullPrompt)
                    .call()
                    .content();
            
            return parseListResponse(response);
        } catch (Exception e) {
            log.error("Failed to generate list output", e);
            throw new RuntimeException("列表输出生成失败", e);
        }
    }

    public Map<String, Object> generateMap(String prompt) {
        try {
            String format = "请返回一个JSON对象，包含键值对。格式：{\"key1\":\"value1\",\"key2\":\"value2\",...}";
            
            String fullPrompt = prompt + "\n\n" + format;
            
            ChatClient chatClient = ChatClient.builder(chatModel).build();
            String response = chatClient.prompt()
                    .user(fullPrompt)
                    .call()
                    .content();
            
            return parseMapResponse(response);
        } catch (Exception e) {
            log.error("Failed to generate map output", e);
            throw new RuntimeException("Map输出生成失败", e);
        }
    }

    public <T> T generateWithTypeReference(String prompt, ParameterizedTypeReference<T> typeReference) {
        try {
            BeanOutputConverter<T> converter = new BeanOutputConverter<>(typeReference);
            String format = converter.getFormat();
            
            String fullPrompt = prompt + "\n\n" + format;
            
            ChatClient chatClient = ChatClient.builder(chatModel).build();
            String response = chatClient.prompt()
                    .user(fullPrompt)
                    .call()
                    .content();
            
            return converter.convert(response);
        } catch (Exception e) {
            log.error("Failed to generate structured output with type reference", e);
            throw new RuntimeException("结构化输出生成失败", e);
        }
    }

    private List<Map<String, Object>> parseListResponse(String response) {
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            int start = response.indexOf("[");
            int end = response.lastIndexOf("]");
            if (start != -1 && end != -1 && end > start) {
                String jsonArray = response.substring(start, end + 1);
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                result = mapper.readValue(jsonArray, new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
            }
        } catch (Exception e) {
            log.warn("Failed to parse list response, returning empty list", e);
        }
        return result;
    }

    private Map<String, Object> parseMapResponse(String response) {
        Map<String, Object> result = new HashMap<>();
        try {
            int start = response.indexOf("{");
            int end = response.lastIndexOf("}");
            if (start != -1 && end != -1 && end > start) {
                String jsonObject = response.substring(start, end + 1);
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                result = mapper.readValue(jsonObject, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            }
        } catch (Exception e) {
            log.warn("Failed to parse map response, returning empty map", e);
        }
        return result;
    }
}
