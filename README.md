<div align="center">

# 🧠 Doc Ingestion Pipeline
### Reactive GraphRAG + Model Context Protocol (MCP) Knowledge Engine

[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3.3-6DB33F?logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring_AI-1.0.0--M1-6DB33F?logo=spring&logoColor=white)](https://spring.io/projects/spring-ai)
[![Neo4j](https://img.shields.io/badge/Neo4j-Graph_DB-008CC1?logo=neo4j&logoColor=white)](https://neo4j.com/)
[![OpenAI](https://img.shields.io/badge/OpenAI-GPT--4o-412991?logo=openai&logoColor=white)](https://openai.com/)
[![WebFlux](https://img.shields.io/badge/Spring_WebFlux-Reactive-6DB33F?logo=spring&logoColor=white)](https://docs.spring.io/spring-framework/reference/web/webflux.html)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

A **production-ready**, fully reactive document ingestion and query pipeline that transforms raw text and PDFs into a rich, queryable **Knowledge Graph + Vector Store** — powered by **Spring AI**, **Neo4j**, **Project Reactor**, and exposed as **MCP (Model Context Protocol)** tools for AI agents.

[Features](#-features) · [Architecture](#-architecture) · [API Reference](#-api-reference) · [Quick Start](#-quick-start) · [Configuration](#-configuration)

</div>

---

## ✨ Features

| Feature | Description |
|---|---|
| 🔀 **Hybrid GraphRAG** | Combines dense vector similarity search with multi-hop Neo4j graph traversal for richer context retrieval |
| 📄 **PDF & Text Ingestion** | Reactive endpoints to ingest raw text or upload PDF files with automatic text extraction via Apache PDFBox |
| 🤖 **AI Entity Extraction** | GPT-4o automatically extracts named entities and semantic relationships from every ingested document chunk |
| 🔌 **MCP Server** | Exposes GraphRAG capabilities as Model Context Protocol tools, making the knowledge base directly usable by AI agents |
| ⚡ **Fully Reactive** | Built on Spring WebFlux + Project Reactor — non-blocking I/O from HTTP request to Neo4j write |
| 🧩 **Pluggable Chunking** | Multiple chunking strategies: sliding window, sentence-aware, and LlamaIndex-style hierarchical chunking |
| 📡 **SSE Streaming** | Real-time Server-Sent Events streaming endpoint for live query responses |
| 🛡️ **Spring Retry** | Automatic retry logic for transient failures in AI API calls and graph operations |
| 🧪 **Testcontainers** | Integration tests spin up a real Neo4j instance in Docker — no mocking the database |

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                     Doc Ingestion Pipeline                          │
│                                                                     │
│  ┌──────────────┐    ┌─────────────────────────────────────────┐   │
│  │   REST API   │    │              MCP Server                  │   │
│  │  (WebFlux)   │    │    (graphrag-knowledge-server v1.0.0)    │   │
│  └──────┬───────┘    └──────────────────┬───────────────────────┘  │
│         │                               │                           │
│  ┌──────▼───────────────────────────────▼──────────────────────┐   │
│  │                   Service Layer                              │   │
│  │                                                              │   │
│  │  ┌──────────────────────┐  ┌──────────────────────────┐     │   │
│  │  │  KnowledgeGraphService│  │    HybridRagService       │     │   │
│  │  │  - Ingest documents  │  │  - Vector-only queries    │     │   │
│  │  │  - Save chunks       │  │  - GraphRAG queries       │     │   │
│  │  │  - Extract entities  │  │  - Agentic RAG (Advisor)  │     │   │
│  │  │  - Save relationships│  │  - SSE Streaming          │     │   │
│  │  └──────────┬───────────┘  └───────────┬──────────────┘     │   │
│  │             │                          │                     │   │
│  │  ┌──────────▼──────────────────────────▼──────────────┐     │   │
│  │  │              GraphContextAdvisor                    │     │   │
│  │  │  Intercepts ChatClient calls to auto-inject graph   │     │   │
│  │  │  context into AI prompts (RequestResponseAdvisor)   │     │   │
│  │  └─────────────────────────────────────────────────────┘    │   │
│  └──────────────────────────────────────────────────────────────┘   │
│                                                                     │
│  ┌────────────────────────┐  ┌────────────────────────────────┐    │
│  │    Chunking Strategies │  │   AI Entity Extraction         │    │
│  │  - TextChunker         │  │   - JsonEntityExtractor        │    │
│  │  - SentenceSplitter    │  │     (GPT-4o → JSON → Graph)    │    │
│  │  - SentenceWindow      │  │   - Prompt templates (.st)     │    │
│  │  - Hierarchical        │  │                                │    │
│  └────────────────────────┘  └────────────────────────────────┘    │
│                                                                     │
│  ┌────────────────────────────────────────────────────────────┐    │
│  │                        Neo4j                               │    │
│  │   DocumentChunk nodes ←──CONTAINS──→ Entity nodes          │    │
│  │   Entity ────[RELATIONSHIP]────→ Entity                    │    │
│  │   + Vector Index (text-embedding-3-small, 1536 dims)       │    │
│  └────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────┘
```

### Data Flow — Ingestion

```
Document / PDF
     │
     ▼
[Chunking Strategy]  ←── Sliding window (500 tokens, 50 overlap)
     │
     ├──▶ Neo4j DocumentChunk node  (graph persistence)
     │
     ├──▶ Neo4j Vector Store        (embedding: text-embedding-3-small)
     │
     └──▶ AI Entity Extraction
               │
               ├──▶ Entity nodes  (name, type, description)
               └──▶ Relationship edges  (source, target, type, weight)
```

### Data Flow — Query

```
User Question
     │
     ├──▶ [Vector Search]  Top-K semantically similar chunks
     │
     ├──▶ [Graph Traversal]  Entity lookup + multi-hop neighborhood expansion
     │
     └──▶ [GPT-4o]  Synthesizes both contexts via system prompt template
               │
               └──▶ Answer
```

---

## 📦 Project Structure

```
doc-ingestion-pipeline/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/example/graphrag/
    │   │   ├── SpringAiGraphragMcpApplication.java      # Entry point
    │   │   ├── advisor/
    │   │   │   └── GraphContextAdvisor.java              # Auto-injects graph context into AI prompts
    │   │   ├── chunking/
    │   │   │   ├── ChunkingStrategy.java                 # Strategy interface
    │   │   │   ├── TextChunker.java                      # Sliding-window chunker
    │   │   │   ├── LlamaIndexSentenceSplitter.java       # Sentence-aware chunking
    │   │   │   ├── LlamaIndexSentenceWindowChunker.java
    │   │   │   └── LlamaIndexHierarchicalChunker.java    # Hierarchical chunking
    │   │   ├── config/
    │   │   │   ├── McpServerConfig.java                  # MCP server configuration
    │   │   │   ├── Neo4jConfig.java                      # Neo4j driver configuration
    │   │   │   └── OpenAiConfig.java                     # OpenAI / Spring AI configuration
    │   │   ├── controller/
    │   │   │   ├── IngestionController.java               # POST /api/ingest (text + PDF)
    │   │   │   └── QueryController.java                  # GET /api/query/*
    │   │   ├── domain/
    │   │   │   ├── DocumentChunk.java                    # Neo4j node (chunk of text)
    │   │   │   ├── Entity.java                           # Neo4j node (extracted entity)
    │   │   │   └── Relationship.java                     # Neo4j relationship
    │   │   ├── extraction/
    │   │   │   ├── EntityExtractor.java                  # Extractor interface
    │   │   │   └── JsonEntityExtractor.java              # GPT-4o → JSON → Neo4j
    │   │   ├── mcp/
    │   │   │   └── McpKnowledgeTools.java                # MCP tool beans
    │   │   ├── repository/
    │   │   │   └── Neo4jGraphRepository.java             # Cypher queries
    │   │   └── service/
    │   │       ├── KnowledgeGraphService.java            # Ingestion orchestration
    │   │       └── HybridRagService.java                 # Query orchestration
    │   └── resources/
    │       ├── application.yml                            # Base configuration
    │       ├── application-dev.yml                       # Dev profile (local Neo4j)
    │       ├── application-prod.yml                      # Prod profile
    │       └── prompts/
    │           ├── entity-extraction.st                  # Entity extraction prompt
    │           ├── relationship-extraction.st             # Relationship extraction prompt
    │           └── graph-rag-system.st                   # GraphRAG system prompt
    └── test/
        └── java/com/example/graphrag/
            ├── SpringAiGraphragMcpApplicationTests.java
            ├── mcp/
            │   └── McpKnowledgeToolsTest.java
            └── service/
                ├── KnowledgeGraphServiceTest.java
                └── HybridRagServiceTest.java
```

---

## 🚀 Quick Start

### Prerequisites

| Requirement | Version |
|---|---|
| Java | 21+ |
| Maven | 3.9+ |
| Neo4j | 5.x (local or AuraDB) |
| Docker | Latest (for Testcontainers) |
| OpenAI API Key | GPT-4o access |

### 1. Clone the repository

```bash
git clone https://github.com/your-username/doc-ingestion-pipeline.git
cd doc-ingestion-pipeline
```

### 2. Configure environment variables

```bash
export NEO4J_URI=bolt://localhost:7687
export NEO4J_USERNAME=neo4j
export NEO4J_PASSWORD=your-password
export OPENAI_API_KEY=sk-...
```

### 3. Start Neo4j (Docker)

```bash
docker run --name neo4j-graphrag \
  -p 7474:7474 -p 7687:7687 \
  -e NEO4J_AUTH=neo4j/your-password \
  -d neo4j:5
```

### 4. Run the application

```bash
# Development profile (default)
./mvnw spring-boot:run

# Production profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

The server starts on **`http://localhost:8088`**.

### 5. Health check

```bash
curl http://localhost:8088/api/query/status
```

```json
{
  "status": "UP",
  "reactiveEngine": "Spring WebFlux / Project Reactor",
  "service": "doc-ingestion-pipeline",
  "version": "1.0.0",
  "mcpServer": "graphrag-knowledge-server",
  "port": 8088
}
```

---

## 📡 API Reference

### Ingestion Endpoints

#### `POST /api/ingest` — Ingest raw text

```bash
curl -X POST http://localhost:8088/api/ingest \
  -H "Content-Type: application/json" \
  -d '{
    "source": "my-document",
    "content": "Spring AI is a framework for building AI-powered applications..."
  }'
```

```json
{
  "status": "SUCCESS",
  "source": "my-document",
  "chunksIngested": 3,
  "message": "Successfully ingested text into Neo4j vector store and knowledge graph"
}
```

---

#### `POST /api/ingest/pdf` — Ingest a PDF file

```bash
curl -X POST http://localhost:8088/api/ingest/pdf \
  -F "file=@/path/to/document.pdf" \
  -F "source=my-pdf-doc"
```

```json
{
  "status": "SUCCESS",
  "source": "my-pdf-doc",
  "pagesProcessed": 12,
  "chunksIngested": 47,
  "message": "Successfully parsed and ingested PDF into Neo4j vector and graph store"
}
```

---

### Query Endpoints

#### `GET /api/query/vector` — Vector-only semantic search

```bash
curl "http://localhost:8088/api/query/vector?q=What+is+Spring+AI?"
```

#### `GET /api/query/graph` — Hybrid GraphRAG (vector + graph)

```bash
curl "http://localhost:8088/api/query/graph?q=How+does+GraphRAG+work?"
```

#### `GET /api/query/graph/stream` — Streaming GraphRAG (Server-Sent Events)

```bash
curl -N "http://localhost:8088/api/query/graph/stream?q=Explain+knowledge+graphs"
```

#### `GET /api/query/agentic` — Agentic RAG (auto graph context injection via Advisor)

```bash
curl "http://localhost:8088/api/query/agentic?q=What+entities+relate+to+Spring+AI?"
```

---

### Knowledge Graph Endpoints

#### `GET /api/query/entities/name/{name}` — Look up entity by name

```bash
curl http://localhost:8088/api/query/entities/name/Spring%20AI
```

#### `GET /api/query/entities/type/{type}` — Find entities by type

```bash
curl http://localhost:8088/api/query/entities/type/TECHNOLOGY
```

#### `GET /api/query/entities/{id}/neighbors` — Graph neighborhood traversal

```bash
curl "http://localhost:8088/api/query/entities/abc123/neighbors?maxHops=2"
```

---

## 🔌 MCP (Model Context Protocol) Tools

This service acts as an **MCP server** named `graphrag-knowledge-server`. AI agents and Claude Desktop can connect to it and use the following tools:

| Tool Name | Description |
|---|---|
| `graph_rag_query` | Query the hybrid GraphRAG knowledge base combining vector embeddings and graph traversal |
| `lookup_graph_entities` | Look up knowledge graph entities and their neighborhood by entity name |
| `ingest_knowledge_document` | Ingest text and extract entities directly into the vector store and knowledge graph |

### Connecting an AI Agent

Configure your MCP client to connect to:
```
http://localhost:8088/mcp
```

---

## ⚙️ Configuration

### `application.yml` Key Settings

```yaml
spring:
  neo4j:
    uri: ${NEO4J_URI:bolt://localhost:7687}
    authentication:
      username: ${NEO4J_USERNAME:neo4j}
      password: ${NEO4J_PASSWORD:password}

  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4o          # LLM for extraction & answering
          temperature: 0.2
      embedding:
        options:
          model: text-embedding-3-small   # 1536 dimensions

    vectorstore:
      neo4j:
        index-name: document-embeddings
        embedding-dimension: 1536
        distance-type: cosine
        label: DocumentChunk

    mcp:
      server:
        name: graphrag-knowledge-server
        version: 1.0.0

graphrag:
  chunking:
    size: 500       # Characters per chunk
    overlap: 50     # Overlap between consecutive chunks

server:
  port: ${PORT:8088}
```

### Environment Variables

| Variable | Required | Default | Description |
|---|---|---|---|
| `OPENAI_API_KEY` | ✅ Yes | — | OpenAI API key (GPT-4o access required) |
| `NEO4J_URI` | ✅ Yes | `bolt://localhost:7687` | Neo4j connection URI |
| `NEO4J_USERNAME` | ✅ Yes | `neo4j` | Neo4j username |
| `NEO4J_PASSWORD` | ✅ Yes | — | Neo4j password |
| `PORT` | No | `8088` | HTTP server port |
| `OPENAI_BASE_URL` | No | `https://api.openai.com` | Custom OpenAI-compatible base URL |

---

## 🧩 Chunking Strategies

The pipeline supports multiple text chunking strategies, selectable at runtime:

| Strategy | Class | Description |
|---|---|---|
| **Sliding Window** | `TextChunker` | Fixed-size chunks with configurable overlap (default: 500 chars, 50 overlap) |
| **Sentence Splitter** | `LlamaIndexSentenceSplitter` | Sentence-boundary-aware splitting, inspired by LlamaIndex |
| **Sentence Window** | `LlamaIndexSentenceWindowChunker` | Single sentences with surrounding context window |
| **Hierarchical** | `LlamaIndexHierarchicalChunker` | Multi-level chunking for parent-child document hierarchy |

---

## 🧪 Testing

The project uses **Testcontainers** to spin up a real Neo4j instance in Docker for integration tests — no in-memory mocking.

```bash
# Run all tests (requires Docker)
./mvnw test

# Run a specific test class
./mvnw test -Dtest=KnowledgeGraphServiceTest

# Skip tests
./mvnw package -DskipTests
```

### Test Coverage

| Test Class | Coverage Area |
|---|---|
| `KnowledgeGraphServiceTest` | Document ingestion, entity storage, neighbor traversal |
| `HybridRagServiceTest` | Vector retrieval, hybrid RAG, agentic queries |
| `McpKnowledgeToolsTest` | MCP tool beans, tool schema registration |
| `SpringAiGraphragMcpApplicationTests` | Application context load |

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| **Language** | Java 21 (Records, Sealed types) |
| **Framework** | Spring Boot 3.3.3 |
| **Reactive** | Spring WebFlux, Project Reactor (Mono/Flux) |
| **AI Orchestration** | Spring AI 1.0.0-M1 |
| **LLM** | OpenAI GPT-4o |
| **Embeddings** | OpenAI `text-embedding-3-small` (1536 dims) |
| **Graph Database** | Neo4j 5.x |
| **Vector Store** | Neo4j Vector Index (cosine similarity) |
| **PDF Parsing** | Apache PDFBox 2.0.31 |
| **Protocol** | Model Context Protocol (MCP) |
| **Testing** | JUnit 5, Testcontainers (Neo4j), Reactor Test |
| **Build** | Maven 3.9+ |
| **Resilience** | Spring Retry |
| **Boilerplate** | Lombok |

---

## 📋 Building for Production

```bash
# Build JAR
./mvnw clean package -DskipTests

# Run production JAR
java -jar target/doc-ingestion-pipeline-1.0.0.jar \
  --spring.profiles.active=prod \
  --OPENAI_API_KEY=sk-... \
  --NEO4J_URI=neo4j+s://your-aura-instance.neo4j.io \
  --NEO4J_USERNAME=neo4j \
  --NEO4J_PASSWORD=your-password
```

### Docker (optional)

```dockerfile
FROM eclipse-temurin:21-jre-alpine
COPY target/doc-ingestion-pipeline-1.0.0.jar app.jar
EXPOSE 8088
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

```bash
docker build -t doc-ingestion-pipeline:1.0.0 .
docker run -p 8088:8088 \
  -e OPENAI_API_KEY=sk-... \
  -e NEO4J_URI=bolt://host.docker.internal:7687 \
  -e NEO4J_PASSWORD=password \
  doc-ingestion-pipeline:1.0.0
```

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Commit your changes: `git commit -m 'feat: add amazing feature'`
4. Push to the branch: `git push origin feature/my-feature`
5. Open a Pull Request



<div align="center">
  Built with ☕ Java 21 · 🌿 Spring AI · 🔗 Neo4j · ⚛️ Project Reactor
</div>
