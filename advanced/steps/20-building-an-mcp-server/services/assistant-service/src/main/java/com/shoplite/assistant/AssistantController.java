package com.shoplite.assistant;

import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shoplite.assistant.AssistantService.AssistantReply;

@Tag(name = "Assistant", description = "Ask the shopping assistant about products (CUSTOMER)")
@RestController
@RequestMapping("/api/assistant")
public class AssistantController {

    private final AssistantService assistant;

    public AssistantController(AssistantService assistant) {
        this.assistant = assistant;
    }

    @Operation(summary = "Send a message to the assistant",
            description = "Leave conversationId out to start a conversation; send the returned id to continue it.")
    @PostMapping("/chat")
    public AssistantReply chat(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ChatMessage message) {
        return assistant.ask(jwt.getClaimAsString("preferred_username"), message.conversationId(), message.message());
    }

    /** A message is capped at 1000 characters: long prompts cost model time and invite prompt stuffing. */
    public record ChatMessage(UUID conversationId, @NotBlank @Size(max = 1000) String message) {
    }

}
