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
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Controller
public class IntranetController {

    private static final Logger logger = LoggerFactory.getLogger(IntranetController.class);

    private final HandbookService handbookService;
    private final WebClient ollamaClient;
    private final String bearerToken;
    private final ObjectMapper objectMapper;
    private Environment environment;

    @Autowired
    public IntranetController(
            HandbookService handbookService, 
            @Value("${ollama.endpoint}") String ollamaEndpoint,
            @Value("${ollama.api.key}") String apiKey,
            ObjectMapper objectMapper,
            Environment environment) {
        this.handbookService = handbookService;
        this.bearerToken = apiKey;
        this.ollamaClient = WebClient.create(ollamaEndpoint);
        this.objectMapper = objectMapper;
        this.environment = environment;
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
        Object chatRequest;
        
        if (environment.matchesProfiles("cloud")) {
            chatRequest = new CloudChatRequest("gemma2:2b", context, userMessage);
        } else {
            String fullPrompt = "Context:\n" + context + "\n\nUser Query: " + userMessage + "\n\nAnswer based on the above context:";
            chatRequest = new LocalChatRequest("gemma2:2b", fullPrompt, false);
        }

        try {
            logger.info("Request body: {}", objectMapper.writeValueAsString(chatRequest));
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize request body", e);
        }

        Map<String, Object> response = ollamaClient.post()
                .headers(headers -> headers.setBearerAuth(bearerToken))
                .bodyValue(chatRequest)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (environment.matchesProfiles("cloud")) {
            // Transform cloud response to match local format
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> firstChoice = choices.get(0);
                Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
                String content = (String) message.get("content");
                
                return Map.of(
                    "model", response.get("model"),
                    "response", content,
                    "done", true,
                    "done_reason", firstChoice.get("finish_reason")
                );
            }
        }

        return response;
    }

    // Local request format
    private static class LocalChatRequest {
        private String model;
        private String prompt;
        private boolean stream;

        public LocalChatRequest(String model, String prompt, boolean stream) {
            this.model = model;
            this.prompt = prompt;
            this.stream = stream;
        }

        public String getModel() { return model; }
        public String getPrompt() { return prompt; }
        public boolean isStream() { return stream; }
    }

    // Cloud request format
    private static class CloudChatRequest {
        private String model;
        private List<Message> messages;
        private boolean stream = false;

        public CloudChatRequest(String model, String context, String userMessage) {
            this.model = model;
            this.messages = Arrays.asList(
                new Message("system", "Use the following context to answer the user's query:\n\n" + context),
                new Message("user", userMessage)
            );
        }

        public String getModel() { return model; }
        public List<Message> getMessages() { return messages; }
        public boolean isStream() { return stream; }
    }

    private static class Message {
        private String role;
        private String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() { return role; }
        public String getContent() { return content; }
    }
}