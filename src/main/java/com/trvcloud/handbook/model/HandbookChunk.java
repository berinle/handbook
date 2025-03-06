package com.trvcloud.handbook.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "handbook_chunks")
@Data
public class HandbookChunk {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chunk_text", nullable = false)
    private String chunkText;

//    @Column(name = "embedding", columnDefinition = "vector")
//    @Type(value = VectorType)
//    private float[] embedding;

    @Column(name = "embedding", columnDefinition = "vector")
    @JdbcTypeCode(SqlTypes.VECTOR)
    private float[] embedding;

//    @Column(name = "embedding")
//    private float[] embedding;
}