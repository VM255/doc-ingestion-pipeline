package com.example.graphrag.service;

import com.example.graphrag.advisor.GraphContextAdvisor;
import com.example.graphrag.domain.Entity;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class HybridRagService {

    private final KnowledgeGraphService knowledgeGraphService;
    private final VectorStore vectorStore;
    private final ChatModel chatModel;
    private final ChatClient.Builder chatClientBuilder;

    @Value("classpath:/prompts/graph-rag-system.st")
    private Resource graphRagSystemPromptResource;

    @Autowired
    public HybridRagService(
            KnowledgeGraphService knowledgeGraphService,
            @Autowired(required = false) VectorStore vectorStore,
            @Autowired(required = false) @org.springframework.context.annotation.Lazy ChatModel chatModel,
            @Autowired(required = false) ChatClient.Builder chatClientBuilder) {
        this.knowledgeGraphService = knowledgeGraphService;
        this.vectorStore = vectorStore;
        this.chatModel = chatModel;
        this.chatClientBuilder = chatClientBuilder;
    }

    public HybridRagService(KnowledgeGraphService knowledgeGraphService,
                            VectorStore vectorStore,
                            ChatModel chatModel) {
        this(knowledgeGraphService, vectorStore, chatModel, null);
    }

    public String query(String userQuery) {
        String vectorContext = "";
        if (vectorStore != null) {
            List<Document> similarDocuments = vectorStore.similaritySearch(
                SearchRequest.query(userQuery).withTopK(3)
            );
            vectorContext = similarDocuments.stream()
                .map(Document::getContent)
                .collect(Collectors.joining("\n"));
        }

        List<Entity> entities = retrieveRelevantEntities(userQuery);
        String graphContext = entities.stream()
            .map(e -> String.format("Entity: %s (%s) - %s", e.name(), e.type(), e.description()))
            .collect(Collectors.joining("\n"));

        String systemPromptText;
        if (graphRagSystemPromptResource != null && graphRagSystemPromptResource.exists()) {
            PromptTemplate template = new PromptTemplate(graphRagSystemPromptResource);
            systemPromptText = template.render(Map.of(
                "vectorContext", vectorContext.isBlank() ? "None" : vectorContext,
                "graphContext", graphContext.isBlank() ? "None" : graphContext
            ));
        } else {
            systemPromptText = String.format("""
                Vector Context:
                %s
                
                Graph Context:
                %s
                """, vectorContext, graphContext);
        }

        String fullPrompt = systemPromptText + "\n\nQuery: " + userQuery;

        if (chatModel != null) {
            return chatModel.call(new Prompt(fullPrompt)).getResult().getOutput().getContent();
        }
        return "Augmented GraphRAG Query Result for: " + userQuery;
    }

    public Mono<String> queryReactive(String userQuery) {
        return Mono.fromCallable(() -> query(userQuery))
                .subscribeOn(Schedulers.boundedElastic());
    }

    public String askVectorOnly(String query) {
        if (vectorStore == null) {
            return "Vector store unavailable.";
        }
        List<Document> docs = vectorStore.similaritySearch(SearchRequest.query(query).withTopK(5));
        String context = docs.stream().map(Document::getContent).collect(Collectors.joining("\n\n"));

        if (chatModel != null) {
            String prompt = String.format("Context:\n%s\n\nQuestion: %s", context, query);
            return chatModel.call(new Prompt(prompt)).getResult().getOutput().getContent();
        }
        return context;
    }

    public Mono<String> askVectorOnlyReactive(String query) {
        return Mono.fromCallable(() -> askVectorOnly(query))
                .subscribeOn(Schedulers.boundedElastic());
    }

    public String askGraphRag(String query) {
        return query(query);
    }

    public Mono<String> askGraphRagReactive(String query) {
        return queryReactive(query);
    }

    public String askAgenticRag(String query) {
        if (chatClientBuilder != null) {
            return chatClientBuilder.build()
                    .prompt()
                    .user(query)
                    .advisors(new GraphContextAdvisor(knowledgeGraphService))
                    .call()
                    .content();
        }
        return query(query);
    }

    public Mono<String> askAgenticRagReactive(String query) {
        return Mono.fromCallable(() -> askAgenticRag(query))
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Flux<String> streamGraphRag(String query) {
        return askGraphRagReactive(query).flux();
    }

    public List<Entity> retrieveRelevantEntities(String query) {
        return knowledgeGraphService.findEntityByName(query)
            .map(entity -> knowledgeGraphService.findNeighbors(entity.id(), 1))
            .orElseGet(List::of);
    }
}
