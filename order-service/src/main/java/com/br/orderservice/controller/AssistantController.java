package com.br.orderservice.controller;

import com.br.orderservice.service.ChatService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/assistant")
@Slf4j
public class AssistantController {

    private final ChatService chatService;

    public AssistantController(ChatService chatService) {
        this.chatService = chatService;
    }

    public record AssistantRequest(@NotBlank String question) {}
    public record AssistantResponse(String answer){}

    @PostMapping
    public AssistantResponse ask(@RequestBody @Valid AssistantRequest request) {
        return new AssistantResponse(chatService.getAnswer(request.question));
    }
}
