package com.trvcloud.handbook.service;

import com.trvcloud.handbook.model.HandbookChunk;
import com.trvcloud.handbook.repository.HandbookChunkRepository;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class HandbookService {

    private static final String HANDBOOK_FILE = "src/main/resources/handbook.md";
    // private static final String HANDBOOK_FILE = "classpath:handbook.md";
    private final HandbookChunkRepository repository;
    private final EmbeddingService embeddingService;
    private final Parser markdownParser;
    private final HtmlRenderer htmlRenderer;

    @Autowired
    public HandbookService(HandbookChunkRepository repository, EmbeddingService embeddingService) {
        this.repository = repository;
        this.embeddingService = embeddingService;
        this.markdownParser = Parser.builder().build();
        this.htmlRenderer = HtmlRenderer.builder().build();
    }

    public String getHandbookHtml() throws IOException {
        String mdContent = Files.readString(Paths.get(HANDBOOK_FILE));
        return htmlRenderer.render(markdownParser.parse(mdContent));
    }

    public String getHandbookContent() throws IOException {
        return Files.readString(Paths.get(HANDBOOK_FILE));
    }

    public void updateHandbook(String newContent) throws IOException {
        Files.writeString(Paths.get(HANDBOOK_FILE), newContent);
        updateEmbeddings(newContent);
    }

    private void updateEmbeddings(String text) {
        List<String> chunks = chunkText(text, 1000); // Using 1000 as a reasonable chunk size
//        repository.deleteAll();
        repository.myDeleteAll();

        for (String chunk : chunks) {
            HandbookChunk handbookChunk = new HandbookChunk();
            handbookChunk.setChunkText(chunk);
            handbookChunk.setEmbedding(embeddingService.getEmbedding(chunk));
//            repository.save(handbookChunk);
            repository.saveCustom(handbookChunk.getChunkText(), handbookChunk.getEmbedding());
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
        List<HandbookChunk> chunks = repository.findTopNByEmbeddingSimilarity(queryEmbedding, topN);
        return String.join("\n\n", chunks.stream().map(HandbookChunk::getChunkText).toList());
    }
}