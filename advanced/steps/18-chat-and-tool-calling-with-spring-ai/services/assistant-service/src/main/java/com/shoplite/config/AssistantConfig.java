package com.shoplite.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.service.registry.ImportHttpServices;

import com.shoplite.assistant.CatalogClient;
import com.shoplite.assistant.CatalogTools;

/** The assistant's ChatClient: system prompt, catalog tools and conversation memory, set once for every request. */
@Configuration(proxyBeanMethods = false)
@ImportHttpServices(group = "catalog", types = CatalogClient.class)
public class AssistantConfig {

    @Bean
    ChatClient assistantChatClient(ChatClient.Builder builder, CatalogTools catalogTools, ChatMemory chatMemory,
            @Value("classpath:prompts/assistant-system.st") Resource systemPrompt) {
        return builder
                .defaultSystem(systemPrompt)
                .defaultTools(catalogTools)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

}
