package com.example.graphrag.chunking;

import com.example.graphrag.domain.DocumentChunk;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Native Java implementation of LlamaIndex's SentenceSplitter.
 * Splits text into sentences and packs them into chunks up to chunkSize with chunkOverlap,
 * ensuring sentences are never broken in the middle.
 */
@org.springframework.context.annotation.Primary
@Component
public class LlamaIndexSentenceSplitter implements ChunkingStrategy {

    private static final Pattern SENTENCE_PATTERN = Pattern.compile("(?<=[.!?\\n])\\s+");
    private static final Pattern PARAGRAPH_PATTERN = Pattern.compile("\\n\\n+");

    private final int chunkSize;
    private final int chunkOverlap;

    public LlamaIndexSentenceSplitter(
            @Value("${graphrag.chunking.llamaindex.size:512}") int chunkSize,
            @Value("${graphrag.chunking.llamaindex.overlap:64}") int chunkOverlap) {
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
    }

    @Override
    public String getStrategyName() {
        return "LLAMAINDEX_SENTENCE_SPLITTER";
    }

    @Override
    public List<DocumentChunk> chunk(String documentId, String content, Map<String, Object> metadata) {
        if (content == null || content.isBlank()) {
            return List.of();
        }

        List<String> sentences = extractSentences(content);
        List<String> textChunks = buildChunksFromSentences(sentences, chunkSize, chunkOverlap);

        List<DocumentChunk> documentChunks = new ArrayList<>();
        for (int i = 0; i < textChunks.size(); i++) {
            Map<String, Object> chunkMeta = new HashMap<>(metadata);
            chunkMeta.put("nodeIndex", i);
            chunkMeta.put("totalNodes", textChunks.size());
            chunkMeta.put("strategy", getStrategyName());
            chunkMeta.put("chunkSize", chunkSize);
            chunkMeta.put("chunkOverlap", chunkOverlap);

            documentChunks.add(new DocumentChunk(
                UUID.randomUUID().toString(),
                documentId,
                textChunks.get(i).trim(),
                chunkMeta
            ));
        }

        return documentChunks;
    }

    private List<String> extractSentences(String text) {
        List<String> sentences = new ArrayList<>();
        String[] paragraphs = PARAGRAPH_PATTERN.split(text);

        for (String para : paragraphs) {
            String trimmedPara = para.trim();
            if (trimmedPara.isEmpty()) continue;

            String[] rawSentences = SENTENCE_PATTERN.split(trimmedPara);
            for (String sentence : rawSentences) {
                String s = sentence.trim();
                if (!s.isEmpty()) {
                    sentences.add(s);
                }
            }
        }
        return sentences;
    }

    private List<String> buildChunksFromSentences(List<String> sentences, int maxChars, int overlapChars) {
        List<String> chunks = new ArrayList<>();
        if (sentences.isEmpty()) {
            return chunks;
        }

        int currentStart = 0;
        while (currentStart < sentences.size()) {
            StringBuilder currentChunk = new StringBuilder();
            int currentLength = 0;
            int lastIncluded = currentStart;

            for (int i = currentStart; i < sentences.size(); i++) {
                String sentence = sentences.get(i);
                int additional = (currentLength > 0 ? 1 : 0) + sentence.length();

                if (currentLength + additional <= maxChars || currentLength == 0) {
                    if (currentLength > 0) {
                        currentChunk.append(" ");
                    }
                    currentChunk.append(sentence);
                    currentLength += additional;
                    lastIncluded = i;
                } else {
                    break;
                }
            }

            chunks.add(currentChunk.toString());

            // Compute next start index based on overlap
            int nextStart = lastIncluded + 1;
            int overlapCount = 0;
            for (int i = lastIncluded; i > currentStart; i--) {
                overlapCount += sentences.get(i).length() + 1;
                if (overlapCount <= overlapChars) {
                    nextStart = i;
                } else {
                    break;
                }
            }

            if (nextStart <= currentStart) {
                nextStart = currentStart + 1;
            }
            currentStart = nextStart;
        }

        return chunks;
    }
}
