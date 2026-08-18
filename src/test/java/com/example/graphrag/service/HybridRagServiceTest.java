package com.example.graphrag.service;

import com.example.graphrag.domain.Entity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HybridRagServiceTest {

    @Mock
    private KnowledgeGraphService knowledgeGraphService;

    @Mock
    private VectorStore vectorStore;

    @Mock
    private ChatModel chatModel;

    private HybridRagService hybridRagService;

    @BeforeEach
    void setUp() {
        hybridRagService = new HybridRagService(knowledgeGraphService, vectorStore, chatModel);
    }

    @Test
    void testHybridRagQueryWithMockedLlmAndVectorStoreAssertions() {
        String userQuery = "What is Spring AI MCP integration?";

        // 1. Mock Vector Store response
        Document doc1 = new Document("Spring AI provides unified interfaces for AI development.");
        Document doc2 = new Document("MCP stands for Model Context Protocol.");
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(doc1, doc2));

        // 2. Mock Knowledge Graph response
        Entity springAiEntity = new Entity("e1", userQuery, "FRAMEWORK", "AI Engineering framework", Map.of());
        Entity neighbor = new Entity("e2", "MCP Protocol", "PROTOCOL", "Model Context Protocol", Map.of());
        when(knowledgeGraphService.findEntityByName(userQuery)).thenReturn(Optional.of(springAiEntity));
        when(knowledgeGraphService.findNeighbors("e1", 1)).thenReturn(List.of(neighbor));

        // 3. Mock LLM ChatModel response
        String expectedAnswer = "Spring AI MCP integration allows models to use GraphRAG tools.";
        Generation generation = new Generation(expectedAnswer);
        ChatResponse mockChatResponse = new ChatResponse(List.of(generation));
        when(chatModel.call(any(Prompt.class))).thenReturn(mockChatResponse);

        // Execute query
        String actualAnswer = hybridRagService.query(userQuery);

        // Assertions
        assertEquals(expectedAnswer, actualAnswer);

        // Verify Vector Store search was executed with query
        ArgumentCaptor<SearchRequest> searchRequestCaptor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore, times(1)).similaritySearch(searchRequestCaptor.capture());
        SearchRequest capturedRequest = searchRequestCaptor.getValue();
        assertEquals(userQuery, capturedRequest.getQuery());
        assertEquals(3, capturedRequest.getTopK());

        // Verify Knowledge Graph was queried
        verify(knowledgeGraphService, times(1)).findEntityByName(userQuery);
        verify(knowledgeGraphService, times(1)).findNeighbors("e1", 1);

        // Verify LLM ChatModel received augmented prompt with both vector & graph contexts
        ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel, times(1)).call(promptCaptor.capture());
        String promptText = promptCaptor.getValue().getContents();

        assertTrue(promptText.contains("Vector Context:"), "Prompt must include vector context header");
        assertTrue(promptText.contains("Spring AI provides unified interfaces"), "Prompt must contain vector content");
        assertTrue(promptText.contains("Graph Context:"), "Prompt must include graph context header");
        assertTrue(promptText.contains("MCP Protocol"), "Prompt must contain graph entity neighbor");
        assertTrue(promptText.contains(userQuery), "Prompt must contain user query");
    }
}
