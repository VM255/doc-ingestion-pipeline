# Spring AI GraphRAG MCP

A Spring Boot application integrating **GraphRAG** (Knowledge Graph + Retrieval-Augmented Generation) with Spring AI and the **Model Context Protocol (MCP)**.

## Project Structure

```
spring-ai-graphrag-mcp/
├── pom.xml
├── README.md
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/
    │   │       └── example/
    │   │           └── graphrag/
    │   │               ├── SpringAiGraphragMcpApplication.java
    │   │               ├── config/
    │   │               │   └── McpServerConfig.java
    │   │               ├── controller/
    │   │               │   ├── IngestionController.java
    │   │               │   └── QueryController.java
    │   │               ├── domain/
    │   │               │   ├── DocumentChunk.java
    │   │               │   ├── Entity.java
    │   │               │   └── Relationship.java
    │   │               ├── mcp/
    │   │               │   └── McpKnowledgeTools.java
    │   │               └── service/
    │   │                   ├── KnowledgeGraphService.java
    │   │                   └── HybridRagService.java
    │   └── resources/
    │       └── application.yml
    └── test/
        └── java/
            └── com/
                └── example/
                    └── graphrag/
                        ├── SpringAiGraphragMcpApplicationTests.java
                        ├── service/
                        │   ├── KnowledgeGraphServiceTest.java
                        │   └── HybridRagServiceTest.java
                        └── mcp/
                            └── McpKnowledgeToolsTest.java
```

## Features

- **GraphRAG Architecture**: Combines vector retrieval with entity and relationship graph search.
- **Spring AI**: Unified interface for AI models and vector stores.
- **Model Context Protocol (MCP)**: Exposes GraphRAG knowledge tools as MCP endpoints.
- **REST Endpoints**: Ingestion and query endpoints for managing knowledge and running hybrid RAG queries.
