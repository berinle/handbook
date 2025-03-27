package com.trvcloud.handbook.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import com.fasterxml.jackson.core.JsonProcessingException;

@Service
public class EmbeddingService {
    private static final Logger logger = LoggerFactory.getLogger(EmbeddingService.class);
    private final WebClient webClient;
    private final String bearerToken;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final EmbeddingRequestBuilder requestBuilder;

    public EmbeddingService(@Value("${embedding.endpoint}") String embeddingEndpoint,
                           @Value("${embedding.api.key}") String apiKey,
                           EmbeddingRequestBuilder requestBuilder) {
        this.bearerToken = apiKey;
        this.webClient = WebClient.builder()
            .baseUrl(embeddingEndpoint)
            .filter((request, next) -> {
                logger.info("Making request to: {}", request.url());
                logger.debug("Request body: {}", request.body());
                return next.exchange(request);
            })
            .build();
        this.requestBuilder = requestBuilder;
    }

    public float[] getEmbedding(String text) {
        Object requestBody = requestBuilder.build("nomic-embed-text", text);
        try {
            // Print the request body as JSON
            logger.info("Request body as JSON: {}", objectMapper.writeValueAsString(requestBody));
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize request body", e);
        }

        return webClient.post()
                .headers(headers -> headers.setBearerAuth(bearerToken))
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(status -> status.is4xxClientError(), response -> {
                    logger.error("Client error: {} {}", response.statusCode(), response.toString());
                    return response.bodyToMono(String.class)
                        .flatMap(body -> {
                            logger.error("Error body: {}", body);
                            return Mono.error(new RuntimeException("Embedding service client error: " + body));
                        });
                })
                .bodyToMono(EmbeddingResponse.class)
                // .map(EmbeddingResponse::getEmbedding)
                .map(response -> {
                    float[] embedding = response.getEmbedding();
                    if (embedding == null) {
                        logger.error("No embedding found in response");
                        throw new RuntimeException("No embedding found in response");
                    }
                    return embedding;
                })
                .block();
    }

    // Interface for request building
    public interface EmbeddingRequestBuilder {
        Object build(String model, String text);
    }

    // Default implementation (uses "prompt")
    @Component
    @Profile("!cloud")
    public static class DefaultEmbeddingRequestBuilder implements EmbeddingRequestBuilder {
        @Override
        public Object build(String model, String text) {
            logger.info("DefaultEmbeddingRequestBuilder.build called");
            return new DefaultEmbeddingRequest(model, text);
        }
    }

    // Cloud implementation (uses "input")
    @Component
    @Profile("cloud")
    public static class CloudEmbeddingRequestBuilder implements EmbeddingRequestBuilder {
        @Override
        public Object build(String model, String text) {
            logger.info("CloudEmbeddingRequestBuilder.build called");
            return new CloudEmbeddingRequest(model, text);
        }
    }

    // Default request class (with "prompt")
    private static class DefaultEmbeddingRequest {
        private String model;
        private String prompt;

        public DefaultEmbeddingRequest(String model, String prompt) {
            this.model = model;
            this.prompt = prompt;
        }

        public String getModel() { return model; }
        public String getPrompt() { return prompt; }
    }

    // Cloud request class (with "input")
    private static class CloudEmbeddingRequest {
        private String model;
        @JsonProperty("input")
        private String prompt;

        public CloudEmbeddingRequest(String model, String prompt) {
            this.model = model;
            this.prompt = prompt;
        }

        public String getModel() { return model; }
        public String getPrompt() { return prompt; }
    }

    private static class EmbeddingResponse {
        private float[] embedding;
        private List<EmbeddingData> data;

        public float[] getEmbedding() {
            // If direct embedding is present, return it (local case)
            if (embedding != null) {
                return embedding;
            }
            // If data array is present, return the first embedding (cloud case)
            if (data != null && !data.isEmpty()) {
                return data.get(0).embedding;
            }
            return null;
        }

        public void setEmbedding(float[] embedding) {
            this.embedding = embedding;
        }

        public List<EmbeddingData> getData() {
            return data;
        }

        public void setData(List<EmbeddingData> data) {
            this.data = data;
        }
    }

    private static class EmbeddingData {
        private float[] embedding;
        private int index;

        public float[] getEmbedding() {
            return embedding;
        }

        public void setEmbedding(float[] embedding) {
            this.embedding = embedding;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }
    }
}