package com.example.graphrag.extraction;

import com.example.graphrag.domain.DocumentChunk;
import com.example.graphrag.domain.Entity;
import com.example.graphrag.domain.Relationship;

import java.util.List;

public interface EntityExtractor {

    record ExtractionResult(
        List<Entity> entities,
        List<Relationship> relationships
    ) {}

    ExtractionResult extract(DocumentChunk chunk);

    ExtractionResult extractFromText(String text);
}
