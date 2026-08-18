package com.example.graphrag.service;

import com.example.graphrag.chunking.ChunkingStrategy;
import com.example.graphrag.domain.DocumentChunk;
import com.example.graphrag.domain.Entity;
import com.example.graphrag.domain.Relationship;
import com.example.graphrag.extraction.EntityExtractor;
import com.example.graphrag.repository.Neo4jGraphRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class KnowledgeGraphService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeGraphService.class);

    private final Neo4jGraphRepository neo4jGraphRepository;
    private final EntityExtractor entityExtractor;
    private final VectorStore vectorStore;
    private final ChunkingStrategy chunkingStrategy;

    @Autowired
    public KnowledgeGraphService(
            Neo4jGraphRepository neo4jGraphRepository,
            @Autowired(required = false) EntityExtractor entityExtractor,
            @Autowired(required = false) VectorStore vectorStore,
            @Autowired(required = false) ChunkingStrategy chunkingStrategy) {
        this.neo4jGraphRepository = neo4jGraphRepository;
        this.entityExtractor = entityExtractor;
        this.vectorStore = vectorStore;
        this.chunkingStrategy = chunkingStrategy;
    }

    public KnowledgeGraphService(Neo4jGraphRepository neo4jGraphRepository) {
        this(neo4jGraphRepository, null, null, null);
    }

    public Entity saveEntity(Entity entity) {
        return neo4jGraphRepository.saveEntity(entity);
    }

    public Optional<Entity> findEntityById(String id) {
        return neo4jGraphRepository.findEntityById(id);
    }

    public Optional<Entity> findEntityByName(String name) {
        return neo4jGraphRepository.findEntityByName(name);
    }

    public List<Entity> findEntitiesByType(String type) {
        return neo4jGraphRepository.findEntitiesByType(type);
    }

    public Relationship saveRelationship(Relationship relationship) {
        return neo4jGraphRepository.saveRelationship(relationship);
    }

    public List<Entity> findNeighbors(String entityId, int depth) {
        return neo4jGraphRepository.findNeighbors(entityId, depth);
    }

    public List<DocumentChunk> extractAndStoreGraph(DocumentChunk chunk) {
        if (entityExtractor != null) {
            EntityExtractor.ExtractionResult result = entityExtractor.extract(chunk);
            for (Entity entity : result.entities()) {
                Entity saved = neo4jGraphRepository.saveEntity(entity);
                neo4jGraphRepository.linkChunkToEntity(chunk.id(), saved.id());
            }
            for (Relationship relationship : result.relationships()) {
                neo4jGraphRepository.saveRelationship(relationship);
            }
        }
        return List.of(chunk);
    }

    public int ingestDocument(String sourceName, String fullText) {
        if (fullText == null || fullText.isBlank()) {
            return 0;
        }

        List<DocumentChunk> chunks;
        if (chunkingStrategy != null) {
            chunks = chunkingStrategy.chunk(sourceName, fullText, Map.of("source", sourceName));
        } else {
            chunks = defaultChunk(sourceName, fullText);
        }

        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk chunk = chunks.get(i);

            // 1. Save DocumentChunk node in Neo4j graph
            neo4jGraphRepository.saveDocumentChunk(chunk, sourceName, i);

            // 2. Index in Vector Store
            if (vectorStore != null) {
                Document doc = new Document(
                    chunk.id(),
                    chunk.content(),
                    Map.of("source", sourceName, "chunkIndex", String.valueOf(i))
                );
                vectorStore.add(List.of(doc));
            }

            // 3. Extract & Link Entities and Relationships
            extractAndStoreGraph(chunk);
        }

        log.info("Successfully ingested document '{}' into Neo4j vector + graph ({}) chunks", sourceName, chunks.size());
        return chunks.size();
    }

    public String retrieveHybridContext(String query, int vectorTopK, int graphDepth) {
        if (vectorStore == null) {
            return "Vector store is not configured.";
        }

        List<Document> anchorDocs = vectorStore.similaritySearch(
            SearchRequest.query(query).withTopK(vectorTopK)
        );

        if (anchorDocs.isEmpty()) {
            return "No relevant vector documents found.";
        }

        StringBuilder context = new StringBuilder();
        context.append("=== SEMANTICALLY SIMILAR CHUNKS ===\n");
        Set<String> chunkIds = new HashSet<>();
        anchorDocs.forEach(doc -> {
            chunkIds.add(doc.getId());
            context.append(String.format("[Chunk %s]: %s\n\n", doc.getId(), doc.getContent()));
        });

        // Subgraph expansion from anchor chunk entities
        List<Map<String, Object>> subgraphPaths = neo4jGraphRepository.expandSubgraphsFromChunks(chunkIds, graphDepth);
        if (!subgraphPaths.isEmpty()) {
            context.append("=== KNOWLEDGE GRAPH EXPANDED PATHS ===\n");
            subgraphPaths.forEach(row ->
                context.append(String.format("- (%s) --[%s]--> (%s) [Hops: %s]\n",
                    row.get("source"),
                    String.join(", ", (List<String>) row.get("relTypes")),
                    row.get("target"),
                    row.get("hops")
                ))
            );
        }

        return context.toString();
    }

    private List<DocumentChunk> defaultChunk(String sourceName, String text) {
        List<DocumentChunk> chunks = new ArrayList<>();
        int chunkSize = 500;
        int overlap = 50;
        int index = 0;

        for (int i = 0; i < text.length(); i += (chunkSize - overlap)) {
            int end = Math.min(i + chunkSize, text.length());
            String chunkText = text.substring(i, end);
            chunks.add(new DocumentChunk(
                UUID.randomUUID().toString(),
                sourceName,
                chunkText,
                Map.of("chunkIndex", index++)
            ));
            if (end == text.length()) break;
        }
        return chunks;
    }
}