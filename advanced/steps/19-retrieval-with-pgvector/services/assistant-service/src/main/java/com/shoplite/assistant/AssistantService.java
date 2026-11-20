package com.shoplite.assistant;

import java.util.UUID;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

import com.shoplite.common.ServiceUnavailableException;

/** Answers one customer message, remembering the earlier messages of the same conversation. */
@Service
public class AssistantService {

    private final ChatClient chatClient;

    public AssistantService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public AssistantReply ask(String username, UUID conversationId, String message) {
        UUID conversation = conversationId != null ? conversationId : UUID.randomUUID();
        try {
            String answer = chatClient.prompt()
                    .user(message)
                    // The memory key includes the user name: nobody can continue someone else's conversation
                    // by guessing its id.
                    .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, username + ":" + conversation))
                    .call()
                    .content();
            return new AssistantReply(conversation, answer);
        }
        catch (ResourceAccessException ex) {
            throw new ServiceUnavailableException("The assistant's language model is not reachable", ex);
        }
    }

    public record AssistantReply(UUID conversationId, String answer) {
    }

}
