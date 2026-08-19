package com.example.graphrag.controller;

import com.example.graphrag.service.KnowledgeGraphService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.HttpHeaders;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class IngestionControllerTest {

    @Test
    void testIngestTextReactive() {
        KnowledgeGraphService mockService = mock(KnowledgeGraphService.class);
        when(mockService.ingestDocumentReactive(anyString(), anyString())).thenReturn(Mono.just(2));

        IngestionController controller = new IngestionController(mockService);

        Mono<Map<String, String>> requestMono = Mono.just(Map.of(
            "source", "reactive-doc",
            "content", "Reactive GraphRAG ingestion with Project Reactor and WebFlux."
        ));

        StepVerifier.create(controller.ingestText(requestMono))
                .assertNext(response -> {
                    assertEquals(200, response.getStatusCode().value());
                    assertNotNull(response.getBody());
                    assertEquals("SUCCESS", response.getBody().get("status"));
                    assertEquals("reactive-doc", response.getBody().get("source"));
                    assertEquals(2, response.getBody().get("chunksIngested"));
                })
                .verifyComplete();
    }

    @Test
    void testIngestPdfSuccessReactive() throws Exception {
        KnowledgeGraphService mockService = mock(KnowledgeGraphService.class);
        when(mockService.ingestDocumentReactive(anyString(), anyString())).thenReturn(Mono.just(3));

        IngestionController controller = new IngestionController(mockService);

        // Generate sample in-memory PDF
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(doc, page)) {
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
                contentStream.newLineAtOffset(100, 700);
                contentStream.showText("Spring AI GraphRAG MCP reactive architecture document.");
                contentStream.endText();
            }
            doc.save(out);
        }

        byte[] pdfBytes = out.toByteArray();
        FilePart mockFilePart = new FilePart() {
            @Override
            public String filename() {
                return "sample.pdf";
            }

            @Override
            public Mono<Void> transferTo(Path dest) {
                return Mono.empty();
            }

            @Override
            public String name() {
                return "file";
            }

            @Override
            public HttpHeaders headers() {
                return HttpHeaders.EMPTY;
            }

            @Override
            public Flux<org.springframework.core.io.buffer.DataBuffer> content() {
                return Flux.just(new DefaultDataBufferFactory().wrap(pdfBytes));
            }
        };

        StepVerifier.create(controller.ingestPdf(Mono.just(mockFilePart), Mono.just("custom-pdf-source")))
                .assertNext(response -> {
                    assertEquals(200, response.getStatusCode().value());
                    assertNotNull(response.getBody());
                    assertEquals("SUCCESS", response.getBody().get("status"));
                    assertEquals("custom-pdf-source", response.getBody().get("source"));
                    assertEquals(3, response.getBody().get("chunksIngested"));
                })
                .verifyComplete();
    }

    @Test
    void testIngestPdfInvalidExtensionReactive() {
        KnowledgeGraphService mockService = mock(KnowledgeGraphService.class);
        IngestionController controller = new IngestionController(mockService);

        FilePart mockFilePart = mock(FilePart.class);
        when(mockFilePart.filename()).thenReturn("invalid.txt");

        StepVerifier.create(controller.ingestPdf(Mono.just(mockFilePart), Mono.empty()))
                .assertNext(response -> {
                    assertEquals(400, response.getStatusCode().value());
                    assertEquals("ERROR", response.getBody().get("status"));
                })
                .verifyComplete();
    }
}
