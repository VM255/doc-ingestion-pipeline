package com.example.graphrag.domain;

import java.util.Map;

public record DocumentChunk(
    String id,
    String documentId,
    String content,
    Map<String, Object> metadata
) {}
