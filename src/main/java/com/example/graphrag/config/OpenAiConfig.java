package com.example.graphrag.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplate;

import java.time.Duration;

@Configuration
public class OpenAiConfig {

    @Bean
    public RetryTemplate llmRetryTemplate() {
        return RetryTemplate.builder()
                .maxAttempts(3)
                .exponentialBackoff(Duration.ofMillis(1000), 2.0, Duration.ofMillis(10000))
                .retryOn(Exception.class)
                .build();
    }

    @Bean
    public ChatClient.Builder chatClientBuilder(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem("You are a helpful assistant powered by a GraphRAG knowledge engine.");
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder chatClientBuilder) {
        return chatClientBuilder.build();
    }
}
