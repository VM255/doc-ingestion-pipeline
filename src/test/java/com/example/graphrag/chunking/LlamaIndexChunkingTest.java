package com.example.graphrag.chunking;

import com.example.graphrag.domain.DocumentChunk;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LlamaIndexChunkingTest {

    private final String sampleText = """
        Spring AI provides a clean framework for developing AI applications. It standardizes model interactions.
        Neo4j is an enterprise-grade graph database and vector store. It enables complex entity traversal and similarity search.
        Model Context Protocol connects AI assistants with live data sources and tools.
        """;

    @Test
    void testLlamaIndexSentenceSplitter() {
        LlamaIndexSentenceSplitter splitter = new LlamaIndexSentenceSplitter(120, 20);
        List<DocumentChunk> chunks = splitter.chunk("doc-1", sampleText);

        assertFalse(chunks.isEmpty());
        for (DocumentChunk chunk : chunks) {
            assertEquals("doc-1", chunk.documentId());
            assertNotNull(chunk.content());
            assertFalse(chunk.content().isBlank());
            assertEquals("LLAMAINDEX_SENTENCE_SPLITTER", chunk.metadata().get("strategy"));
        }
    }

    @Test
    void testLlamaIndexHierarchicalChunker() {
        LlamaIndexHierarchicalChunker chunker = new LlamaIndexHierarchicalChunker(200, 80, 10);
        List<DocumentChunk> chunks = chunker.chunk("doc-2", sampleText);

        assertFalse(chunks.isEmpty());
        boolean hasParent = chunks.stream().anyMatch(c -> "PARENT".equals(c.metadata().get("nodeType")));
        boolean hasChild = chunks.stream().anyMatch(c -> "CHILD".equals(c.metadata().get("nodeType")));

        assertTrue(hasParent, "Should create Parent nodes");
        assertTrue(hasChild, "Should create Child nodes");
    }

    @Test
    void testLlamaIndexSentenceWindowChunker() {
        LlamaIndexSentenceWindowChunker chunker = new LlamaIndexSentenceWindowChunker(1);
        List<DocumentChunk> chunks = chunker.chunk("doc-3", sampleText);

        assertEquals(5, chunks.size(), "Should have 5 individual sentence chunks");
        for (DocumentChunk chunk : chunks) {
            assertTrue(chunk.metadata().containsKey("window"));
            String window = (String) chunk.metadata().get("window");
            assertTrue(window.contains(chunk.content()), "Window context must include target sentence");
        }
    }
}
