package com.example.graphrag.repository;

import com.example.graphrag.domain.DocumentChunk;
import com.example.graphrag.domain.Entity;
import com.example.graphrag.domain.Relationship;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Query;
import org.neo4j.driver.Record;
import org.neo4j.driver.Values;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Repository
public class Neo4jGraphRepository {

    private final Driver driver;

    public Neo4jGraphRepository(Driver driver) {
        this.driver = driver;
    }

    public void saveDocumentChunk(DocumentChunk chunk, String sourceName, int index) {
        try (var session = driver.session()) {
            session.executeWrite(tx -> {
                String cypher = """
                    MERGE (c:DocumentChunk {id: $chunkId})
                    SET c.text = $text,
                        c.source = $source,
                        c.index = $index,
                        c.updatedAt = datetime()
                    RETURN c
                """;
                tx.run(new Query(cypher, Values.parameters(
                    "chunkId", chunk.id(),
                    "text", chunk.content(),
                    "source", sourceName != null ? sourceName : chunk.documentId(),
                    "index", index
                )));
                return null;
            });
        }
    }

    public void linkChunkToEntity(String chunkId, String entityId) {
        try (var session = driver.session()) {
            session.executeWrite(tx -> {
                String cypher = """
                    MATCH (c:DocumentChunk {id: $chunkId})
                    MATCH (e:Entity {id: $entityId})
                    MERGE (c)-[:CONTAINS]->(e)
                """;
                tx.run(new Query(cypher, Values.parameters("chunkId", chunkId, "entityId", entityId)));
                return null;
            });
        }
    }

    public Entity saveEntity(Entity entity) {
        try (var session = driver.session()) {
            return session.executeWrite(tx -> {
                String cypher = """
                    MERGE (e:Entity {name: $name})
                    ON CREATE SET e.id = $id,
                                  e.type = $type,
                                  e.description = $description,
                                  e.createdAt = datetime()
                    ON MATCH SET e.description = CASE WHEN $description <> '' THEN $description ELSE e.description END,
                                 e.updatedAt = datetime()
                    RETURN e.id AS id, e.name AS name, e.type AS type, e.description AS description
                """;
                var result = tx.run(new Query(cypher, Values.parameters(
                    "id", entity.id(),
                    "name", entity.name(),
                    "type", entity.type(),
                    "description", entity.description() != null ? entity.description() : ""
                )));
                if (result.hasNext()) {
                    return mapRecordToEntity(result.next());
                }
                return entity;
            });
        }
    }

    public Optional<Entity> findEntityById(String id) {
        try (var session = driver.session()) {
            return session.executeRead(tx -> {
                String cypher = "MATCH (e:Entity {id: $id}) RETURN e.id AS id, e.name AS name, e.type AS type, e.description AS description";
                var result = tx.run(new Query(cypher, Values.parameters("id", id)));
                if (result.hasNext()) {
                    return Optional.of(mapRecordToEntity(result.next()));
                }
                return Optional.empty();
            });
        }
    }

    public Optional<Entity> findEntityByName(String name) {
        try (var session = driver.session()) {
            return session.executeRead(tx -> {
                String cypher = "MATCH (e:Entity {name: $name}) RETURN e.id AS id, e.name AS name, e.type AS type, e.description AS description";
                var result = tx.run(new Query(cypher, Values.parameters("name", name)));
                if (result.hasNext()) {
                    return Optional.of(mapRecordToEntity(result.next()));
                }
                return Optional.empty();
            });
        }
    }

    public List<Entity> findEntitiesByType(String type) {
        try (var session = driver.session()) {
            return session.executeRead(tx -> {
                String cypher = "MATCH (e:Entity {type: $type}) RETURN e.id AS id, e.name AS name, e.type AS type, e.description AS description";
                var result = tx.run(new Query(cypher, Values.parameters("type", type)));
                return result.list(this::mapRecordToEntity);
            });
        }
    }

    public Relationship saveRelationship(Relationship relationship) {
        try (var session = driver.session()) {
            return session.executeWrite(tx -> {
                String cypher = """
                    MATCH (source:Entity {id: $sourceId})
                    MATCH (target:Entity {id: $targetId})
                    MERGE (source)-[r:RELATES {type: $relType}]->(target)
                    ON CREATE SET r.id = $id,
                                  r.description = $description,
                                  r.weight = $weight,
                                  r.createdAt = datetime()
                    ON MATCH SET r.weight = $weight,
                                 r.updatedAt = datetime()
                    RETURN r.id AS id, $sourceId AS sourceId, $targetId AS targetId, $relType AS type, r.description AS description, r.weight AS weight
                """;
                tx.run(new Query(cypher, Values.parameters(
                    "sourceId", relationship.sourceEntityId(),
                    "targetId", relationship.targetEntityId(),
                    "relType", relationship.type(),
                    "id", relationship.id(),
                    "description", relationship.description() != null ? relationship.description() : "",
                    "weight", relationship.weight() != null ? relationship.weight() : 1.0
                )));
                return relationship;
            });
        }
    }

    public List<Entity> findNeighbors(String entityId, int depth) {
        int boundedDepth = Math.max(1, Math.min(depth, 3));
        try (var session = driver.session()) {
            return session.executeRead(tx -> {
                String cypher = String.format("""
                    MATCH (e:Entity {id: $id})-[*1..%d]-(neighbor:Entity)
                    RETURN DISTINCT neighbor.id AS id, neighbor.name AS name, neighbor.type AS type, neighbor.description AS description
                """, boundedDepth);
                var result = tx.run(new Query(cypher, Values.parameters("id", entityId)));
                return result.list(this::mapRecordToEntity);
            });
        }
    }

    public List<Map<String, Object>> expandSubgraphsFromChunks(Set<String> chunkIds, int depth) {
        int boundedDepth = Math.max(1, Math.min(depth, 3));
        String cypher = String.format("""
            MATCH (c:DocumentChunk)-[:CONTAINS]->(seed:Entity)
            WHERE c.id IN $chunkIds
            WITH collect(DISTINCT seed) AS seeds
            UNWIND seeds AS seed
            MATCH path = (seed)-[:RELATES*1..%d]-(connected:Entity)
            WITH seed, connected, path
            ORDER BY length(path)
            RETURN seed.name AS source,
                   connected.name AS target,
                   length(path) AS hops,
                   [rel IN relationships(path) | type(rel)] AS relTypes
            LIMIT 25
        """, boundedDepth);

        try (var session = driver.session()) {
            return session.executeRead(tx -> {
                var result = tx.run(new Query(cypher, Values.parameters("chunkIds", chunkIds)));
                return result.list(record -> Map.of(
                    "source", record.get("source").asString(""),
                    "target", record.get("target").asString(""),
                    "hops", record.get("hops").asInt(1),
                    "relTypes", record.get("relTypes").asList(v -> v.asString(""))
                ));
            });
        }
    }

    public void deleteEntity(String id) {
        try (var session = driver.session()) {
            session.executeWrite(tx -> {
                tx.run(new Query("MATCH (e:Entity {id: $id}) DETACH DELETE e", Values.parameters("id", id)));
                return null;
            });
        }
    }

    private Entity mapRecordToEntity(Record record) {
        return new Entity(
            record.get("id").asString(),
            record.get("name").asString(),
            record.get("type").asString(),
            record.get("description").asString(""),
            Map.of()
        );
    }
}
