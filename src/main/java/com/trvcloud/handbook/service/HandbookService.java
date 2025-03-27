package com.trvcloud.handbook.service;

import com.trvcloud.handbook.model.HandbookChunk;
import com.trvcloud.handbook.repository.HandbookChunkRepository;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

@Service
public class HandbookService {

    private static final String HANDBOOK_FILE = "classpath:handbook.md";
    private final HandbookChunkRepository repository;
    private final EmbeddingService embeddingService;
    private final Parser markdownParser;
    private final HtmlRenderer htmlRenderer;
    private final ResourceLoader resourceLoader;

    @Autowired
    public HandbookService(HandbookChunkRepository repository, 
                         EmbeddingService embeddingService,
                         ResourceLoader resourceLoader) {
        this.repository = repository;
        this.embeddingService = embeddingService;
        this.resourceLoader = resourceLoader;
        this.markdownParser = Parser.builder().build();
        this.htmlRenderer = HtmlRenderer.builder().build();
    }

    public String getHandbookHtml() throws IOException {
        String mdContent = getHandbookContent();
        return htmlRenderer.render(markdownParser.parse(mdContent));
    }

    public String getHandbookContent() throws IOException {
        Resource resource = resourceLoader.getResource(HANDBOOK_FILE);
        try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            return FileCopyUtils.copyToString(reader);
        }
    }

    @Transactional
    public void updateHandbook(String newContent) throws IOException {
        Resource resource = resourceLoader.getResource(HANDBOOK_FILE);
        try {
            Files.writeString(resource.getFile().toPath(), newContent);
            updateEmbeddings(newContent);
        } catch (IOException e) {
            // Log a warning that file writing isn't supported in this environment
            // In Cloud Foundry, you might want to store content in database instead
            throw new UnsupportedOperationException(
                "Direct file writing not supported in this environment. Consider storing content in database.", e);
        }
    }

    private void updateEmbeddings(String text) {
        List<String> chunks = chunkText(text, 1000);
        repository.deleteAll();

        for (String chunk : chunks) {
            HandbookChunk handbookChunk = new HandbookChunk();
            handbookChunk.setChunkText(chunk);
            float[] embedding = embeddingService.getEmbedding(chunk);
            String vectorString = "[" + String.join(",", java.util.stream.IntStream.range(0, embedding.length)
                .mapToObj(i -> String.format("%.6f", embedding[i]))
                .toList()) + "]";
            repository.saveCustom(chunk, vectorString);
        }
    }

    private List<String> chunkText(String text, int chunkSize) {
        List<String> chunks = new ArrayList<>();
        String[] paragraphs = text.split("\n\n");
        for (String para : paragraphs) {
            para = para.trim();
            if (para.isEmpty()) continue;
            if (para.length() > chunkSize) {
                String[] words = para.split("\\s+");
                StringBuilder currentChunk = new StringBuilder();
                for (String word : words) {
                    if (currentChunk.length() + word.length() + 1 > chunkSize) {
                        chunks.add(currentChunk.toString().trim());
                        currentChunk = new StringBuilder(word).append(" ");
                    } else {
                        currentChunk.append(word).append(" ");
                    }
                }
                if (!currentChunk.isEmpty()) chunks.add(currentChunk.toString().trim());
            } else {
                chunks.add(para);
            }
        }
        return chunks;
    }

    public String retrieveContext(String query, int topN) {
        float[] queryEmbedding = embeddingService.getEmbedding(query);
        // Format the vector string with brackets for PostgreSQL
        String vectorString = "[" + String.join(",", java.util.stream.IntStream.range(0, queryEmbedding.length)
            .mapToObj(i -> String.format("%.6f", queryEmbedding[i]))
            .toList()) + "]";
        List<HandbookChunk> chunks = repository.findTopNByEmbeddingSimilarity(vectorString, topN);
        return String.join("\n\n", chunks.stream().map(HandbookChunk::getChunkText).toList());
    }
}