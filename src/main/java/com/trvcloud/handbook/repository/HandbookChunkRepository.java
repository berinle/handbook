package com.trvcloud.handbook.repository;

import com.trvcloud.handbook.model.HandbookChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface HandbookChunkRepository extends JpaRepository<HandbookChunk, Long> {
    @Query(value = "SELECT * FROM handbook_chunks ORDER BY embedding <-> (:queryEmbedding)::vector LIMIT :topN", nativeQuery = true)
    List<HandbookChunk> findTopNByEmbeddingSimilarity(@Param("queryEmbedding") String queryEmbedding, @Param("topN") int topN);

    // @Transactional
    @Modifying
    @Query(value = "INSERT INTO handbook_chunks (chunk_text, embedding) VALUES (:chunkText, (:embedding)::vector)", nativeQuery = true)
    void saveCustom(@Param("chunkText") String chunkText, @Param("embedding") String embedding);

    // @Transactional
    @Modifying
    @Query(value ="delete from handbook_chunks", nativeQuery = true)
    void myDeleteAll();
}