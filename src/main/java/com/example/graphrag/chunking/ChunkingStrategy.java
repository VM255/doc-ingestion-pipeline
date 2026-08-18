package com.example.graphrag.chunking;

import com.example.graphrag.domain.DocumentChunk;

import java.util.List;
import java.util.Map;

public interface ChunkingStrategy {

    String getStrategyName();

    List<DocumentChunk> chunk(String documentId, String content, Map<String, Object> metadata);

    default List<DocumentChunk> chunk(String documentId, String content) {
        return chunk(documentId, content, Map.of());
    }
}
