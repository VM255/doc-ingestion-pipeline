package com.example.graphrag.service;

import com.example.graphrag.domain.Entity;
import com.example.graphrag.domain.Relationship;
import com.example.graphrag.repository.Neo4jGraphRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class KnowledgeGraphServiceTest {

    private static Driver driver;
    private KnowledgeGraphService knowledgeGraphService;

    @BeforeAll
    static void initDriver() {
        try {
            driver = GraphDatabase.driver(
                "bolt://localhost:7688",
                AuthTokens.basic("neo4j", "password")
            );
            driver.verifyConnectivity();
        } catch (Exception e) {
            // Fallback for isolated CI runs
            driver = null;
        }
    }

    @AfterAll
    static void closeDriver() {
        if (driver != null) {
            driver.close();
        }
    }

    @BeforeEach
    void setUp() {
        org.junit.jupiter.api.Assumptions.assumeTrue(driver != null, "Neo4j driver must be connected to run integration tests");
        Neo4jGraphRepository repository = new Neo4jGraphRepository(driver);
        knowledgeGraphService = new KnowledgeGraphService(repository);
        try (var session = driver.session()) {
            session.run("MATCH (n) DETACH DELETE n");
        }
    }

    @Test
    void testGraphWriteAndReadEntity() {
        Entity entity = new Entity(
            "entity-1",
            "Spring AI",
            "FRAMEWORK",
            "An application framework for AI engineering",
            Map.of("category", "AI")
        );

        Entity saved = knowledgeGraphService.saveEntity(entity);
        assertNotNull(saved);
        assertEquals("Spring AI", saved.name());

        Optional<Entity> retrieved = knowledgeGraphService.findEntityByName("Spring AI");
        assertTrue(retrieved.isPresent(), "Entity should be present in Neo4j after write");
        assertEquals("entity-1", retrieved.get().id());
        assertEquals("FRAMEWORK", retrieved.get().type());
        assertEquals("An application framework for AI engineering", retrieved.get().description());
    }

    @Test
    void testGraphWriteRelationshipsAndTraverseNeighbors() {
        Entity springAi = new Entity("e1", "Spring AI", "FRAMEWORK", "AI framework", Map.of());
        Entity neo4j = new Entity("e2", "Neo4j", "DATABASE", "Graph database", Map.of());
        Entity mcp = new Entity("e3", "MCP", "PROTOCOL", "Model Context Protocol", Map.of());

        knowledgeGraphService.saveEntity(springAi);
        knowledgeGraphService.saveEntity(neo4j);
        knowledgeGraphService.saveEntity(mcp);

        Relationship rel1 = new Relationship("r1", "e1", "e2", "INTEGRATES_WITH", "Uses for graph queries", 1.0, Map.of());
        Relationship rel2 = new Relationship("r2", "e1", "e3", "SUPPORTS", "Implements MCP tools", 1.0, Map.of());

        knowledgeGraphService.saveRelationship(rel1);
        knowledgeGraphService.saveRelationship(rel2);

        List<Entity> neighbors = knowledgeGraphService.findNeighbors("e1", 1);
        assertEquals(2, neighbors.size(), "Spring AI should have 2 connected neighbors in Neo4j");

        List<String> neighborNames = neighbors.stream().map(Entity::name).toList();
        assertTrue(neighborNames.contains("Neo4j"));
        assertTrue(neighborNames.contains("MCP"));
    }
}
