package com.trvcloud.handbook.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class EmbeddingService {

    private final WebClient webClient;

    public EmbeddingService(@Value("${embedding.endpoint}") String embeddingEndpoint) {
        this.webClient = WebClient.create(embeddingEndpoint);
    }

    public float[] getEmbedding(String text) {
        return webClient.post()
                .bodyValue(new EmbeddingRequest("nomic-embed-text", text))
                .retrieve()
                .bodyToMono(EmbeddingResponse.class)
                .map(EmbeddingResponse::getEmbedding)
                .block(); // Blocking for simplicity; consider async in production
    }

    private static class EmbeddingRequest {
        private String model;
        private String prompt;

        public EmbeddingRequest(String model, String prompt) {
            this.model = model;
            this.prompt = prompt;
        }

        public String getModel() { return model; }
        public String getPrompt() { return prompt; }
    }

    private static class EmbeddingResponse {
        private float[] embedding;

        public float[] getEmbedding() { return embedding; }
        public void setEmbedding(float[] embedding) { this.embedding = embedding; }
    }
}