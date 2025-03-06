package com.trvcloud.handbook.repository;

import com.trvcloud.handbook.model.HandbookChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface HandbookChunkRepository extends JpaRepository<HandbookChunk, Long> {
     @Query(value = "SELECT * FROM handbook_chunks ORDER BY embedding <-> CAST(?1 AS vector) LIMIT ?2", nativeQuery = true)
//    @Query(value = "SELECT * FROM handbook_chunks ORDER BY embedding <-> ?1::vector LIMIT ?2", nativeQuery = true)
    List<HandbookChunk> findTopNByEmbeddingSimilarity(float[] queryEmbedding, int limit);

    @Query(value = "INSERT INTO handbook_chunks (chunk_text, embedding) VALUES (:text, CAST(:embedding AS vector)) RETURNING *", nativeQuery = true)
     Long saveCustom(@Param("text") String text, @Param("embedding") float[] embedding); //HandbookChunk handbookChunk);

    @Transactional
    @Modifying
    @Query(value ="delete from handbook_chunks", nativeQuery = true)
    void myDeleteAll();
}