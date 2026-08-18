package com.example.graphrag.mcp;

import com.example.graphrag.domain.Entity;
import com.example.graphrag.service.HybridRagService;
import com.example.graphrag.service.KnowledgeGraphService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class McpKnowledgeToolsTest {

    @Mock
    private HybridRagService hybridRagService;

    @Mock
    private KnowledgeGraphService knowledgeGraphService;

    private McpKnowledgeTools mcpKnowledgeTools;

    @BeforeEach
    void setUp() {
        mcpKnowledgeTools = new McpKnowledgeTools(hybridRagService, knowledgeGraphService);
    }

    @Test
    void testVerifyToolSchemaRegistration() {
        List<McpKnowledgeTools.ToolSchema> registeredTools = mcpKnowledgeTools.getRegisteredTools();

        assertNotNull(registeredTools);
        assertEquals(3, registeredTools.size(), "Should have 3 MCP tools registered");

        // Verify GraphRAG query tool registration
        var graphRagToolOpt = registeredTools.stream()
                .filter(t -> McpKnowledgeTools.TOOL_GRAPH_RAG_QUERY.equals(t.name()))
                .findFirst();
        assertTrue(graphRagToolOpt.isPresent(), "graph_rag_query tool must be registered");
        var graphRagTool = graphRagToolOpt.get();
        assertEquals(McpKnowledgeTools.QueryRequest.class, graphRagTool.inputType());
        assertEquals(McpKnowledgeTools.QueryResponse.class, graphRagTool.returnType());
        assertNotNull(graphRagTool.description());

        // Verify Entity lookup tool registration
        var lookupToolOpt = registeredTools.stream()
                .filter(t -> McpKnowledgeTools.TOOL_LOOKUP_ENTITIES.equals(t.name()))
                .findFirst();
        assertTrue(lookupToolOpt.isPresent(), "lookup_graph_entities tool must be registered");
        var lookupTool = lookupToolOpt.get();
        assertEquals(McpKnowledgeTools.EntityLookupRequest.class, lookupTool.inputType());
        assertEquals(McpKnowledgeTools.EntityLookupResponse.class, lookupTool.returnType());

        // Verify Ingest document tool registration
        var ingestToolOpt = registeredTools.stream()
                .filter(t -> McpKnowledgeTools.TOOL_INGEST_DOCUMENT.equals(t.name()))
                .findFirst();
        assertTrue(ingestToolOpt.isPresent(), "ingest_knowledge_document tool must be registered");
        var ingestTool = ingestToolOpt.get();
        assertEquals(McpKnowledgeTools.IngestDocumentRequest.class, ingestTool.inputType());
        assertEquals(McpKnowledgeTools.IngestDocumentResponse.class, ingestTool.returnType());
    }

    @Test
    void testGraphRagQueryToolExecution() {
        when(hybridRagService.query("Explain Neo4j GraphRAG")).thenReturn("Neo4j stores knowledge nodes.");

        var queryFn = mcpKnowledgeTools.graphRagQuery();
        var response = queryFn.apply(new McpKnowledgeTools.QueryRequest("Explain Neo4j GraphRAG"));

        assertNotNull(response);
        assertEquals("Neo4j stores knowledge nodes.", response.answer());
        verify(hybridRagService, times(1)).query("Explain Neo4j GraphRAG");
    }

    @Test
    void testLookupEntitiesToolExecution() {
        Entity entity = new Entity("e1", "Neo4j", "DATABASE", "Graph DB", Map.of());
        when(knowledgeGraphService.findEntityByName("Neo4j")).thenReturn(Optional.of(entity));

        var lookupFn = mcpKnowledgeTools.lookupEntities();
        var response = lookupFn.apply(new McpKnowledgeTools.EntityLookupRequest("Neo4j"));

        assertNotNull(response);
        assertEquals(1, response.entities().size());
        assertEquals("Neo4j", response.entities().get(0).name());
        verify(knowledgeGraphService, times(1)).findEntityByName("Neo4j");
    }

    @Test
    void testIngestDocumentToolExecution() {
        when(knowledgeGraphService.ingestDocument("manual-doc", "Sample text content")).thenReturn(2);

        var ingestFn = mcpKnowledgeTools.ingestDocumentTool();
        var response = ingestFn.apply(new McpKnowledgeTools.IngestDocumentRequest("manual-doc", "Sample text content"));

        assertNotNull(response);
        assertEquals("SUCCESS", response.status());
        assertEquals(2, response.chunksIngested());
        verify(knowledgeGraphService, times(1)).ingestDocument("manual-doc", "Sample text content");
    }
}
