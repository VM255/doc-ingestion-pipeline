package com.example.graphrag.mcp;

import com.example.graphrag.domain.Entity;
import com.example.graphrag.service.HybridRagService;
import com.example.graphrag.service.KnowledgeGraphService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

@Component
public class McpKnowledgeTools {

    public static final String TOOL_GRAPH_RAG_QUERY = "graph_rag_query";
    public static final String TOOL_LOOKUP_ENTITIES = "lookup_graph_entities";
    public static final String TOOL_INGEST_DOCUMENT = "ingest_knowledge_document";

    private final HybridRagService hybridRagService;
    private final KnowledgeGraphService knowledgeGraphService;

    public McpKnowledgeTools(@org.springframework.context.annotation.Lazy HybridRagService hybridRagService,
                             @org.springframework.context.annotation.Lazy KnowledgeGraphService knowledgeGraphService) {
        this.hybridRagService = hybridRagService;
        this.knowledgeGraphService = knowledgeGraphService;
    }

    public record ToolSchema(String name, String description, Class<?> inputType, Class<?> returnType) {}

    public record QueryRequest(String query) {}
    public record QueryResponse(String answer) {}

    public record EntityLookupRequest(String entityName) {}
    public record EntityLookupResponse(List<Entity> entities) {}

    public record IngestDocumentRequest(String sourceName, String content) {}
    public record IngestDocumentResponse(String status, int chunksIngested) {}

    public List<ToolSchema> getRegisteredTools() {
        return List.of(
            new ToolSchema(
                TOOL_GRAPH_RAG_QUERY,
                "Query the hybrid GraphRAG knowledge base combining vector embeddings and graph traversal",
                QueryRequest.class,
                QueryResponse.class
            ),
            new ToolSchema(
                TOOL_LOOKUP_ENTITIES,
                "Lookup knowledge graph entities and their neighborhood",
                EntityLookupRequest.class,
                EntityLookupResponse.class
            ),
            new ToolSchema(
                TOOL_INGEST_DOCUMENT,
                "Ingest and extract entities from text into the vector store and knowledge graph",
                IngestDocumentRequest.class,
                IngestDocumentResponse.class
            )
        );
    }

    @Bean(TOOL_GRAPH_RAG_QUERY)
    @Description("Query the hybrid GraphRAG knowledge base combining vector embeddings and graph traversal")
    public Function<QueryRequest, QueryResponse> graphRagQuery() {
        return request -> new QueryResponse(hybridRagService.query(request.query()));
    }

    @Bean(TOOL_LOOKUP_ENTITIES)
    @Description("Lookup knowledge graph entities and their neighborhood")
    public Function<EntityLookupRequest, EntityLookupResponse> lookupEntities() {
        return request -> {
            var entity = knowledgeGraphService.findEntityByName(request.entityName());
            return new EntityLookupResponse(entity.map(List::of).orElseGet(List::of));
        };
    }

    @Bean(TOOL_INGEST_DOCUMENT)
    @Description("Ingest and extract entities from text into the vector store and knowledge graph")
    public Function<IngestDocumentRequest, IngestDocumentResponse> ingestDocumentTool() {
        return request -> {
            int chunks = knowledgeGraphService.ingestDocument(request.sourceName(), request.content());
            return new IngestDocumentResponse("SUCCESS", chunks);
        };
    }
}
