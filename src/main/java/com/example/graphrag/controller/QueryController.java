package com.example.graphrag.controller;

import com.example.graphrag.domain.Entity;
import com.example.graphrag.service.HybridRagService;
import com.example.graphrag.service.KnowledgeGraphService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/query")
public class QueryController {

    private final HybridRagService hybridRagService;
    private final KnowledgeGraphService knowledgeGraphService;

    public QueryController(HybridRagService hybridRagService, KnowledgeGraphService knowledgeGraphService) {
        this.hybridRagService = hybridRagService;
        this.knowledgeGraphService = knowledgeGraphService;
    }

    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        return Map.of(
            "status", "UP",
            "service", "Spring AI GraphRAG MCP Hub",
            "version", "1.0.0",
            "mcpServer", "graphrag-knowledge-server",
            "port", 8088
        );
    }

    @GetMapping("/vector")
    public String vectorOnly(@RequestParam String q) {
        return hybridRagService.askVectorOnly(q);
    }

    @GetMapping("/graph")
    public String graphRag(@RequestParam String q) {
        return hybridRagService.askGraphRag(q);
    }

    @GetMapping("/agentic")
    public String agenticRag(@RequestParam String q) {
        return hybridRagService.askAgenticRag(q);
    }

    @GetMapping("/entities/name/{name}")
    public ResponseEntity<Entity> getEntityByName(@PathVariable String name) {
        return knowledgeGraphService.findEntityByName(name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/entities/type/{type}")
    public List<Entity> getEntitiesByType(@PathVariable String type) {
        return knowledgeGraphService.findEntitiesByType(type);
    }

    @GetMapping("/entities/{id}/neighbors")
    public List<Entity> getEntityNeighbors(@PathVariable String id,
                                          @RequestParam(defaultValue = "1") int maxHops) {
        return knowledgeGraphService.findNeighbors(id, maxHops);
    }
}
