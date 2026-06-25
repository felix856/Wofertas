package com.example.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ChatbotRequest;
import com.example.demo.dto.ChatbotResponse;
import com.example.demo.security.CustomUserDetails;
import com.example.demo.service.ChatbotAssistantService;

@RestController
@RequestMapping({"/chatbot", "/api/chatbot"})
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class ChatbotController {

    private final ChatbotAssistantService chatbotAssistantService;

    public ChatbotController(ChatbotAssistantService chatbotAssistantService) {
        this.chatbotAssistantService = chatbotAssistantService;
    }

    @PostMapping("/mensagem")
    public ResponseEntity<ChatbotResponse> responder(
            @RequestBody ChatbotRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(chatbotAssistantService.responder(request, userDetails));
    }
}
