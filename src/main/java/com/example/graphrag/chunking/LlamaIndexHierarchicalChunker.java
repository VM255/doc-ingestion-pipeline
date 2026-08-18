package com.example.graphrag.chunking;

import com.example.graphrag.domain.DocumentChunk;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Native Java implementation of LlamaIndex's HierarchicalNodeParser (Parent-Child Chunking).
 * Generates hierarchical nodes where leaf/child nodes retain a pointer to their parent node
 * for expanded context during GraphRAG synthesis.
 */
@Component
public class LlamaIndexHierarchicalChunker implements ChunkingStrategy {

    private final int parentChunkSize;
    private final int childChunkSize;
    private final int overlap;

    public LlamaIndexHierarchicalChunker(
            @Value("${graphrag.chunking.parent.size:1024}") int parentChunkSize,
            @Value("${graphrag.chunking.child.size:256}") int childChunkSize,
            @Value("${graphrag.chunking.overlap:32}") int overlap) {
        this.parentChunkSize = parentChunkSize;
        this.childChunkSize = childChunkSize;
        this.overlap = overlap;
    }

    @Override
    public String getStrategyName() {
        return "LLAMAINDEX_HIERARCHICAL_PARENT_CHILD";
    }

    @Override
    public List<DocumentChunk> chunk(String documentId, String content, Map<String, Object> metadata) {
        if (content == null || content.isBlank()) {
            return List.of();
        }

        List<DocumentChunk> resultChunks = new ArrayList<>();
        List<String> parentTexts = splitIntoBlocks(content, parentChunkSize, overlap);

        for (int pIdx = 0; pIdx < parentTexts.size(); pIdx++) {
            String parentContent = parentTexts.get(pIdx);
            String parentId = UUID.randomUUID().toString();

            // Create Parent Node
            Map<String, Object> parentMeta = new HashMap<>(metadata);
            parentMeta.put("nodeType", "PARENT");
            parentMeta.put("nodeIndex", pIdx);
            parentMeta.put("strategy", getStrategyName());

            DocumentChunk parentChunk = new DocumentChunk(
                parentId,
                documentId,
                parentContent,
                parentMeta
            );
            resultChunks.add(parentChunk);

            // Create Child Nodes linked to this Parent Node
            List<String> childTexts = splitIntoBlocks(parentContent, childChunkSize, overlap);
            for (int cIdx = 0; cIdx < childTexts.size(); cIdx++) {
                Map<String, Object> childMeta = new HashMap<>(metadata);
                childMeta.put("nodeType", "CHILD");
                childMeta.put("parentId", parentId);
                childMeta.put("parentIndex", pIdx);
                childMeta.put("childIndex", cIdx);
                childMeta.put("strategy", getStrategyName());

                DocumentChunk childChunk = new DocumentChunk(
                    UUID.randomUUID().toString(),
                    documentId,
                    childTexts.get(cIdx),
                    childMeta
                );
                resultChunks.add(childChunk);
            }
        }

        return resultChunks;
    }

    private List<String> splitIntoBlocks(String text, int blockSize, int blockOverlap) {
        List<String> blocks = new ArrayList<>();
        int start = 0;
        int step = Math.max(1, blockSize - blockOverlap);

        while (start < text.length()) {
            int end = Math.min(start + blockSize, text.length());
            blocks.add(text.substring(start, end).trim());
            start += step;
        }
        return blocks;
    }
}
