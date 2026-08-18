package com.example.graphrag.controller;

import com.example.graphrag.service.KnowledgeGraphService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayOutputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class IngestionControllerTest {

    @Test
    void testIngestPdfSuccess() throws Exception {
        KnowledgeGraphService mockService = mock(KnowledgeGraphService.class);
        when(mockService.ingestDocument(anyString(), anyString())).thenReturn(3);

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
                contentStream.showText("Spring AI GraphRAG MCP enterprise architecture document.");
                contentStream.endText();
            }
            doc.save(out);
        }

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "sample.pdf",
            "application/pdf",
            out.toByteArray()
        );

        ResponseEntity<Map<String, Object>> response = controller.ingestPdf(file, "custom-pdf-source");

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("SUCCESS", response.getBody().get("status"));
        assertEquals("custom-pdf-source", response.getBody().get("source"));
        assertEquals(3, response.getBody().get("chunksIngested"));

        verify(mockService, times(1)).ingestDocument(eq("custom-pdf-source"), contains("Spring AI GraphRAG"));
    }

    @Test
    void testIngestPdfInvalidExtension() {
        KnowledgeGraphService mockService = mock(KnowledgeGraphService.class);
        IngestionController controller = new IngestionController(mockService);

        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "Hello".getBytes());
        ResponseEntity<Map<String, Object>> response = controller.ingestPdf(file, null);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("ERROR", response.getBody().get("status"));
    }
}
