package com.example.graphrag.chunking;

import com.example.graphrag.domain.DocumentChunk;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class TextChunker implements ChunkingStrategy {

    private final int defaultChunkSize;
    private final int defaultChunkOverlap;

    public TextChunker(
            @Value("${graphrag.chunking.size:500}") int defaultChunkSize,
            @Value("${graphrag.chunking.overlap:50}") int defaultChunkOverlap) {
        this.defaultChunkSize = defaultChunkSize;
        this.defaultChunkOverlap = defaultChunkOverlap;
    }

    @Override
    public String getStrategyName() {
        return "RECURSIVE_SEMANTIC";
    }

    @Override
    public List<DocumentChunk> chunk(String documentId, String content, Map<String, Object> metadata) {
        return chunk(documentId, content, metadata, defaultChunkSize, defaultChunkOverlap);
    }

    public List<DocumentChunk> chunk(String documentId, String content, Map<String, Object> metadata, int chunkSize, int chunkOverlap) {
        if (content == null || content.isBlank()) {
            return List.of();
        }

        List<String> rawChunks = splitRecursively(content, chunkSize, chunkOverlap);
        List<DocumentChunk> chunks = new ArrayList<>();

        for (int i = 0; i < rawChunks.size(); i++) {
            Map<String, Object> chunkMetadata = new HashMap<>(metadata);
            chunkMetadata.put("chunkIndex", i);
            chunkMetadata.put("totalChunks", rawChunks.size());
            chunkMetadata.put("strategy", getStrategyName());

            chunks.add(new DocumentChunk(
                UUID.randomUUID().toString(),
                documentId,
                rawChunks.get(i).trim(),
                chunkMetadata
            ));
        }

        return chunks;
    }

    private List<String> splitRecursively(String text, int chunkSize, int chunkOverlap) {
        List<String> result = new ArrayList<>();
        String[] paragraphs = text.split("\n\n+");

        StringBuilder currentChunk = new StringBuilder();

        for (String paragraph : paragraphs) {
            String trimmedPara = paragraph.trim();
            if (trimmedPara.isEmpty()) {
                continue;
            }

            if (currentChunk.length() + trimmedPara.length() + 1 <= chunkSize) {
                if (currentChunk.length() > 0) {
                    currentChunk.append("\n\n");
                }
                currentChunk.append(trimmedPara);
            } else {
                if (currentChunk.length() > 0) {
                    result.add(currentChunk.toString());
                    currentChunk = new StringBuilder();
                }

                if (trimmedPara.length() > chunkSize) {
                    List<String> sentenceChunks = splitBySentences(trimmedPara, chunkSize, chunkOverlap);
                    result.addAll(sentenceChunks);
                } else {
                    currentChunk.append(trimmedPara);
                }
            }
        }

        if (currentChunk.length() > 0) {
            result.add(currentChunk.toString());
        }

        return result;
    }

    private List<String> splitBySentences(String text, int chunkSize, int chunkOverlap) {
        List<String> result = new ArrayList<>();
        String[] sentences = text.split("(?<=[.!?])\\s+");

        StringBuilder current = new StringBuilder();
        for (String sentence : sentences) {
            if (current.length() + sentence.length() + 1 <= chunkSize) {
                if (current.length() > 0) {
                    current.append(" ");
                }
                current.append(sentence);
            } else {
                if (current.length() > 0) {
                    result.add(current.toString());
                    current = new StringBuilder();
                }
                if (sentence.length() > chunkSize) {
                    // Fallback to sliding window by words/characters
                    result.addAll(splitByWords(sentence, chunkSize, chunkOverlap));
                } else {
                    current.append(sentence);
                }
            }
        }

        if (current.length() > 0) {
            result.add(current.toString());
        }

        return result;
    }

    private List<String> splitByWords(String text, int chunkSize, int chunkOverlap) {
        List<String> result = new ArrayList<>();
        int start = 0;
        int step = Math.max(1, chunkSize - chunkOverlap);

        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            result.add(text.substring(start, end));
            start += step;
        }

        return result;
    }
}
