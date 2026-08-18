package com.example.graphrag.chunking;

import com.example.graphrag.domain.DocumentChunk;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Native Java implementation of LlamaIndex's SentenceWindowNodeParser.
 * Splits a document into individual sentences while attaching a surrounding context window
 * (k sentences before and after) in the metadata for expanded synthesis.
 */
@Component
public class LlamaIndexSentenceWindowChunker implements ChunkingStrategy {

    private static final Pattern SENTENCE_SPLIT = Pattern.compile("(?<=[.!?])\\s+");

    private final int windowSize;

    public LlamaIndexSentenceWindowChunker(
            @Value("${graphrag.chunking.window.size:3}") int windowSize) {
        this.windowSize = windowSize;
    }

    @Override
    public String getStrategyName() {
        return "LLAMAINDEX_SENTENCE_WINDOW";
    }

    @Override
    public List<DocumentChunk> chunk(String documentId, String content, Map<String, Object> metadata) {
        if (content == null || content.isBlank()) {
            return List.of();
        }

        String[] rawSentences = SENTENCE_SPLIT.split(content.trim());
        List<String> sentences = new ArrayList<>();
        for (String s : rawSentences) {
            String trimmed = s.trim();
            if (!trimmed.isEmpty()) {
                sentences.add(trimmed);
            }
        }

        List<DocumentChunk> chunks = new ArrayList<>();
        for (int i = 0; i < sentences.size(); i++) {
            String sentence = sentences.get(i);

            int start = Math.max(0, i - windowSize);
            int end = Math.min(sentences.size(), i + windowSize + 1);

            StringBuilder windowBuilder = new StringBuilder();
            for (int w = start; w < end; w++) {
                if (windowBuilder.length() > 0) {
                    windowBuilder.append(" ");
                }
                windowBuilder.append(sentences.get(w));
            }

            Map<String, Object> chunkMeta = new HashMap<>(metadata);
            chunkMeta.put("sentenceIndex", i);
            chunkMeta.put("window", windowBuilder.toString());
            chunkMeta.put("windowSize", windowSize);
            chunkMeta.put("strategy", getStrategyName());

            chunks.add(new DocumentChunk(
                UUID.randomUUID().toString(),
                documentId,
                sentence,
                chunkMeta
            ));
        }

        return chunks;
    }
}
