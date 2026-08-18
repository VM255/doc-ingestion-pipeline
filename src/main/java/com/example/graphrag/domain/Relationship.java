package com.example.graphrag.domain;

import java.util.Map;
import java.util.UUID;

public record Relationship(
    String id,
    String sourceEntityId,
    String targetEntityId,
    String type,
    String description,
    Double weight,
    Map<String, Object> properties
) {
    public Relationship(String fromEntity, String toEntity, String relationType) {
        this(UUID.randomUUID().toString(), fromEntity, toEntity, relationType, "", 1.0, Map.of());
    }

    public String getId() { return id(); }
    public String getFromEntity() { return sourceEntityId(); }
    public String getToEntity() { return targetEntityId(); }
    public String getRelationType() { return type(); }
    public String getDescription() { return description(); }
    public Double getWeight() { return weight(); }
    public Map<String, Object> getProperties() { return properties(); }
}
