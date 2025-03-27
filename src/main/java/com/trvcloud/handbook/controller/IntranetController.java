package com.trvcloud.handbook.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trvcloud.handbook.service.EmbeddingService;
import com.trvcloud.handbook.service.HandbookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.Map;

@Controller
public class IntranetController {

    private static final Logger logger = LoggerFactory.getLogger(IntranetController.class);

    private final HandbookService handbookService;
    private final WebClient ollamaClient;
    private final String bearerToken;
    private final ObjectMapper objectMapper;

    @Autowired
    public IntranetController(
            HandbookService handbookService, 
            @Value("${ollama.endpoint}") String ollamaEndpoint,
            @Value("${ollama.api.key}") String apiKey,
            ObjectMapper objectMapper) {
        this.handbookService = handbookService;
        this.bearerToken = apiKey;
        this.ollamaClient = WebClient.create(ollamaEndpoint);
        this.objectMapper = objectMapper;
    }

    @GetMapping("/")
    public String landing() {
        return "landing";
    }

    @GetMapping("/handbook")
    public String handbook(Model model) throws IOException {
        model.addAttribute("handbook", handbookService.getHandbookHtml());
        return "handbook";
    }

    @GetMapping("/edit-handbook")
    public String editHandbook(Model model) throws IOException {
        model.addAttribute("content", handbookService.getHandbookContent());
        return "edit-handbook";
    }

    @PostMapping("/update-handbook")
    public String updateHandbook(@RequestParam("handbook_content") String newContent) throws IOException {
        handbookService.updateHandbook(newContent);
        return "redirect:/handbook";
    }

    @GetMapping("/chatbot")
    public String chatbot() {
        return "chatbot";
    }

    @PostMapping(value = "/api/chat", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> apiChat(@RequestBody Map<String, String> request) {
        String userMessage = request.get("message");
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return Map.of("error", "No message provided");
        }

        String context = handbookService.retrieveContext(userMessage, 5);
        String fullPrompt = "Context:\n" + context + "\n\nUser Query: " + userMessage + "\n\nAnswer based on the above context:";

        ChatRequest chatRequest = new ChatRequest("gemma2:2b", fullPrompt, false);
        try {
            logger.info("Request body: {}", objectMapper.writeValueAsString(chatRequest));
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize request body", e);
        }

        return ollamaClient.post()
                .headers(headers -> headers.setBearerAuth(bearerToken))
                .bodyValue(chatRequest)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    private static class ChatRequest {
        private String model;
        private String prompt;
        private boolean stream;

        public ChatRequest(String model, String prompt, boolean stream) {
            this.model = model;
            this.prompt = prompt;
            this.stream = stream;
        }

        public String getModel() { return model; }
        public String getPrompt() { return prompt; }
        public boolean isStream() { return stream; }
    }
}