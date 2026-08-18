package com.example.graphrag.advisor;

import com.example.graphrag.domain.Entity;
import com.example.graphrag.service.KnowledgeGraphService;
import org.springframework.ai.chat.client.AdvisedRequest;
import org.springframework.ai.chat.client.RequestResponseAdvisor;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class GraphContextAdvisor implements RequestResponseAdvisor, Ordered {

    public static final String GRAPH_CONTEXT_PARAM = "graphContext";
    private static final int DEFAULT_ORDER = 0;

    private final KnowledgeGraphService knowledgeGraphService;
    private final int order;

    @org.springframework.beans.factory.annotation.Autowired
    public GraphContextAdvisor(KnowledgeGraphService knowledgeGraphService) {
        this(knowledgeGraphService, DEFAULT_ORDER);
    }

    public GraphContextAdvisor(KnowledgeGraphService knowledgeGraphService, int order) {
        this.knowledgeGraphService = knowledgeGraphService;
        this.order = order;
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    @Override
    public AdvisedRequest adviseRequest(AdvisedRequest advisedRequest, Map<String, Object> context) {
        String userText = advisedRequest.userText();
        String graphContext = retrieveGraphContext(userText);

        Map<String, Object> advisedUserParams = new HashMap<>(advisedRequest.userParams());
        advisedUserParams.put(GRAPH_CONTEXT_PARAM, graphContext);

        String augmentedSystemText = (advisedRequest.systemText() != null ? advisedRequest.systemText() : "")
                + "\n\nKnowledge Graph Context:\n" + graphContext;

        return AdvisedRequest.from(advisedRequest)
                .withSystemText(augmentedSystemText.trim())
                .withUserParams(advisedUserParams)
                .build();
    }

    @Override
    public ChatResponse adviseResponse(ChatResponse response, Map<String, Object> context) {
        return response;
    }

    public String retrieveGraphContext(String query) {
        if (query == null || query.isBlank()) {
            return "No graph context available.";
        }

        List<Entity> neighbors = knowledgeGraphService.findEntityByName(query)
                .map(entity -> knowledgeGraphService.findNeighbors(entity.id(), 1))
                .orElseGet(List::of);

        if (neighbors.isEmpty()) {
            return "No relevant graph entities found.";
        }

        return neighbors.stream()
                .map(e -> String.format("- %s (%s): %s", e.name(), e.type(), e.description()))
                .collect(Collectors.joining("\n"));
    }
}
