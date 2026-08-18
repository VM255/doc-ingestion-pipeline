package com.example.graphrag.extraction;

import com.example.graphrag.domain.DocumentChunk;
import com.example.graphrag.domain.Entity;
import com.example.graphrag.domain.Relationship;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class JsonEntityExtractor implements EntityExtractor {

    private static final Logger log = LoggerFactory.getLogger(JsonEntityExtractor.class);

    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;

    @Value("classpath:/prompts/entity-extraction.st")
    private Resource entityExtractionPromptResource;

    @Value("classpath:/prompts/relationship-extraction.st")
    private Resource relationshipExtractionPromptResource;

    public JsonEntityExtractor(@org.springframework.context.annotation.Lazy ChatModel chatModel, ObjectMapper objectMapper) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
    }

    public record RawEntityDto(
        @JsonProperty("name") String name,
        @JsonProperty("type") String type,
        @JsonProperty("description") String description
    ) {}

    public record RawRelationshipDto(
        @JsonProperty("source") String source,
        @JsonProperty("target") String target,
        @JsonProperty("type") String type,
        @JsonProperty("description") String description,
        @JsonProperty("weight") Double weight
    ) {}

    public record RawEntitiesPayload(
        @JsonProperty("entities") List<RawEntityDto> entities
    ) {}

    public record RawRelationshipsPayload(
        @JsonProperty("relationships") List<RawRelationshipDto> relationships
    ) {}

    @Override
    public ExtractionResult extract(DocumentChunk chunk) {
        return extractFromText(chunk.content());
    }

    @Override
    public ExtractionResult extractFromText(String text) {
        if (text == null || text.isBlank()) {
            return new ExtractionResult(List.of(), List.of());
        }

        // 1. Extract Entities using externalized PromptTemplate
        List<Entity> entities = extractEntitiesFromText(text);

        // 2. Extract Relationships using known entities
        List<Relationship> relationships = extractRelationshipsFromText(text, entities);

        return new ExtractionResult(entities, relationships);
    }

    public List<Entity> extractEntitiesFromText(String text) {
        try {
            PromptTemplate template = new PromptTemplate(entityExtractionPromptResource);
            Prompt prompt = template.create(Map.of("text", text));

            String rawJson = chatModel.call(prompt).getResult().getOutput().getContent();
            rawJson = sanitizeJson(rawJson);

            RawEntitiesPayload payload = objectMapper.readValue(rawJson, RawEntitiesPayload.class);
            List<Entity> entities = new ArrayList<>();

            if (payload != null && payload.entities() != null) {
                for (RawEntityDto dto : payload.entities()) {
                    entities.add(new Entity(
                        UUID.randomUUID().toString(),
                        dto.name(),
                        dto.type() != null ? dto.type() : "CONCEPT",
                        dto.description() != null ? dto.description() : "",
                        Map.of()
                    ));
                }
            }
            return entities;
        } catch (Exception e) {
            log.warn("Entity extraction fallback due to: {}", e.getMessage());
            return List.of();
        }
    }

    public List<Relationship> extractRelationshipsFromText(String text, List<Entity> entities) {
        if (entities.size() < 2) {
            return List.of();
        }

        try {
            String entitiesFormatted = entities.stream()
                .map(e -> String.format("- %s (%s)", e.name(), e.type()))
                .collect(Collectors.joining("\n"));

            PromptTemplate template = new PromptTemplate(relationshipExtractionPromptResource);
            Prompt prompt = template.create(Map.of(
                "text", text,
                "entities", entitiesFormatted
            ));

            String rawJson = chatModel.call(prompt).getResult().getOutput().getContent();
            rawJson = sanitizeJson(rawJson);

            RawRelationshipsPayload payload = objectMapper.readValue(rawJson, RawRelationshipsPayload.class);
            List<Relationship> relationships = new ArrayList<>();

            Map<String, String> nameToId = new HashMap<>();
            for (Entity entity : entities) {
                nameToId.put(entity.name().toLowerCase(), entity.id());
            }

            if (payload != null && payload.relationships() != null) {
                for (RawRelationshipDto dto : payload.relationships()) {
                    String sourceId = nameToId.get(dto.source().toLowerCase());
                    String targetId = nameToId.get(dto.target().toLowerCase());

                    if (sourceId != null && targetId != null) {
                        relationships.add(new Relationship(
                            UUID.randomUUID().toString(),
                            sourceId,
                            targetId,
                            dto.type() != null ? dto.type() : "RELATED_TO",
                            dto.description() != null ? dto.description() : "",
                            dto.weight() != null ? dto.weight() : 1.0,
                            Map.of()
                        ));
                    }
                }
            }
            return relationships;
        } catch (Exception e) {
            log.warn("Relationship extraction fallback due to: {}", e.getMessage());
            return List.of();
        }
    }

    private String sanitizeJson(String raw) {
        String trimmed = raw.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
    }
}
