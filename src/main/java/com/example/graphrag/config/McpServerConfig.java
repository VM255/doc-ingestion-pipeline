package com.example.graphrag.config;

import com.example.graphrag.mcp.McpKnowledgeTools;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.model.function.FunctionCallbackWrapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class McpServerConfig {

    @Bean
    public FunctionCallback graphRagQueryCallback(McpKnowledgeTools knowledgeTools) {
        return FunctionCallbackWrapper.builder(knowledgeTools.graphRagQuery())
                .withName(McpKnowledgeTools.TOOL_GRAPH_RAG_QUERY)
                .withDescription("Query the hybrid GraphRAG knowledge base combining vector embeddings and graph traversal")
                .withInputType(McpKnowledgeTools.QueryRequest.class)
                .build();
    }

    @Bean
    public FunctionCallback lookupEntitiesCallback(McpKnowledgeTools knowledgeTools) {
        return FunctionCallbackWrapper.builder(knowledgeTools.lookupEntities())
                .withName(McpKnowledgeTools.TOOL_LOOKUP_ENTITIES)
                .withDescription("Lookup knowledge graph entities and their neighborhood")
                .withInputType(McpKnowledgeTools.EntityLookupRequest.class)
                .build();
    }

    @Bean
    public FunctionCallback ingestDocumentCallback(McpKnowledgeTools knowledgeTools) {
        return FunctionCallbackWrapper.builder(knowledgeTools.ingestDocumentTool())
                .withName(McpKnowledgeTools.TOOL_INGEST_DOCUMENT)
                .withDescription("Ingest and extract entities from text into the vector store and knowledge graph")
                .withInputType(McpKnowledgeTools.IngestDocumentRequest.class)
                .build();
    }

    @Bean
    public List<FunctionCallback> knowledgeToolsCallbacks(FunctionCallback graphRagQueryCallback,
                                                           FunctionCallback lookupEntitiesCallback,
                                                           FunctionCallback ingestDocumentCallback) {
        return List.of(graphRagQueryCallback, lookupEntitiesCallback, ingestDocumentCallback);
    }
}