package com.trvcloud.handbook.controller;

import com.trvcloud.handbook.service.EmbeddingService;
import com.trvcloud.handbook.service.HandbookService;
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

    private final HandbookService handbookService;
    private final WebClient ollamaClient;

    @Autowired
    public IntranetController(HandbookService handbookService, @Value("${ollama.endpoint}") String ollamaEndpoint) {
        this.handbookService = handbookService;
        this.ollamaClient = WebClient.create(ollamaEndpoint);
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

        return ollamaClient.post()
                .bodyValue(new ChatRequest("gemma2:2b", fullPrompt, false))
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