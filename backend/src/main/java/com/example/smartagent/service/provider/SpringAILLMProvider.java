package com.example.smartagent.service.provider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.StreamingChatModel;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
@RequiredArgsConstructor
@Slf4j
public class SpringAILLMProvider implements LLMProvider {

    private final ChatClient chatClient;
    private final ChatModel chatModel;
    private final StreamingChatModel streamingChatModel;

    public SpringAILLMProvider(ChatModel chatModel, StreamingChatModel streamingChatModel) {
        this.chatModel = chatModel;
        this.streamingChatModel = streamingChatModel;
        this.chatClient = ChatClient.builder(chatModel).build();
    }

    @Override
    public String getProviderName() {
        return "springai-openai";
    }

    @Override
    public String generate(String prompt) {
        try {
            ChatResponse response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .chatResponse();

            if (response == null || response.getResult() == null) {
                return "抱歉，生成回答失败";
            }

            return response.getResult().getOutput().getText();
        } catch (Exception e) {
            log.error("Spring AI chat request failed", e);
            return "抱歉，服务暂时不可用";
        }
    }

    @Override
    public String generateWithContext(String prompt, String context) {
        try {
            String fullPrompt = buildRAGPrompt(context, prompt);
            return generate(fullPrompt);
        } catch (Exception e) {
            log.error("Spring AI chat with context failed", e);
            return "抱歉，服务暂时不可用";
        }
    }

    @Override
    public StreamResponse generateStream(String prompt, StreamHandler handler) {
        try {
            Flux<ChatResponse> flux = chatClient.prompt()
                    .user(prompt)
                    .stream()
                    .chatResponse();

            flux.subscribe(
                    response -> {
                        String chunk = response.getResult() != null ?
                                response.getResult().getOutput().getText() : "";
                        if (chunk != null && !chunk.isEmpty()) {
                            handler.onChunk(chunk);
                        }
                    },
                    error -> {
                        log.error("Stream error", error);
                        handler.onError(error);
                    },
                    () -> {
                        log.debug("Stream complete");
                        handler.onComplete();
                    }
            );

            return StreamResponse.started();
        } catch (Exception e) {
            log.error("Spring AI stream request failed", e);
            handler.onError(e);
            return new StreamResponse(false);
        }
    }

    public Flux<String> streamContent(String prompt) {
        return chatClient.prompt()
                .user(prompt)
                .stream()
                .content();
    }

    public Flux<ChatResponse> streamChatResponse(String prompt) {
        return chatClient.prompt()
                .user(prompt)
                .stream()
                .chatResponse();
    }
}
