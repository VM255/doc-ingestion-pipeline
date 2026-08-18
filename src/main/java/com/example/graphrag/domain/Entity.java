package com.example.graphrag.domain;

import java.util.Map;
import java.util.UUID;

public record Entity(
    String id,
    String name,
    String type,
    String description,
    Map<String, Object> properties
) {
    public Entity(String name, String type, String description) {
        this(UUID.randomUUID().toString(), name, type, description, Map.of());
    }

    public String getId() { return id(); }
    public String getName() { return name(); }
    public String getType() { return type(); }
    public String getDescription() { return description(); }
    public Map<String, Object> getProperties() { return properties(); }
}
