package com.example.graphrag.controller;

import com.example.graphrag.domain.Entity;
import com.example.graphrag.service.HybridRagService;
import com.example.graphrag.service.KnowledgeGraphService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
    public Mono<Map<String, Object>> getStatus() {
        return Mono.just(Map.of(
            "status", "UP",
            "reactiveEngine", "Spring WebFlux / Project Reactor",
            "service", "doc-ingestion-pipeline",
            "version", "1.0.0",
            "mcpServer", "graphrag-knowledge-server",
            "port", 8088
        ));
    }

    @GetMapping("/vector")
    public Mono<String> vectorOnly(@RequestParam String q) {
        return hybridRagService.askVectorOnlyReactive(q);
    }

    @GetMapping("/graph")
    public Mono<String> graphRag(@RequestParam String q) {
        return hybridRagService.askGraphRagReactive(q);
    }

    @GetMapping(value = "/graph/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamGraphRag(@RequestParam String q) {
        return hybridRagService.streamGraphRag(q);
    }

    @GetMapping("/agentic")
    public Mono<String> agenticRag(@RequestParam String q) {
        return hybridRagService.askAgenticRagReactive(q);
    }

    @GetMapping("/entities/name/{name}")
    public Mono<ResponseEntity<Entity>> getEntityByName(@PathVariable String name) {
        return knowledgeGraphService.findEntityByNameReactive(name)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/entities/type/{type}")
    public Flux<Entity> getEntitiesByType(@PathVariable String type) {
        return knowledgeGraphService.findEntitiesByTypeReactive(type);
    }

    @GetMapping("/entities/{id}/neighbors")
    public Flux<Entity> getEntityNeighbors(@PathVariable String id,
                                          @RequestParam(defaultValue = "1") int maxHops) {
        return knowledgeGraphService.findNeighborsReactive(id, maxHops);
    }
}
