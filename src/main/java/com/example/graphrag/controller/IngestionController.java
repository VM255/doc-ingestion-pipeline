package com.example.graphrag.controller;

import com.example.graphrag.service.KnowledgeGraphService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@RestController
@RequestMapping("/api/ingest")
public class IngestionController {

    private final KnowledgeGraphService knowledgeGraphService;

    public IngestionController(KnowledgeGraphService knowledgeGraphService) {
        this.knowledgeGraphService = knowledgeGraphService;
    }

    /**
     * Ingest raw text JSON payload.
     * Example: POST /api/ingest
     * Body: { "source": "doc1", "content": "..." }
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> ingestText(@RequestBody Map<String, String> request) {
        String source = request.getOrDefault("source", "raw-text-doc");
        String content = request.get("content");

        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "ERROR",
                "message", "'content' field is required"
            ));
        }

        int chunksIngested = knowledgeGraphService.ingestDocument(source, content);
        return ResponseEntity.ok(Map.of(
            "status", "SUCCESS",
            "source", source,
            "chunksIngested", chunksIngested,
            "message", "Successfully ingested text into Neo4j vector store and knowledge graph"
        ));
    }

    /**
     * Ingest PDF document via multipart file upload.
     * Example: POST /api/ingest/pdf (multipart/form-data with 'file' parameter)
     */
    @PostMapping(value = "/pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> ingestPdf(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "source", required = false) String customSource) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "ERROR",
                "message", "Uploaded PDF file cannot be empty"
            ));
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "ERROR",
                "message", "File must have a .pdf extension"
            ));
        }

        String source = (customSource != null && !customSource.isBlank()) ? customSource : filename;

        try (InputStream inputStream = file.getInputStream();
             PDDocument document = PDDocument.load(inputStream)) {

            if (document.isEncrypted()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "ERROR",
                    "message", "Encrypted/Password-protected PDF files are not supported"
                ));
            }

            PDFTextStripper textStripper = new PDFTextStripper();
            String extractedText = textStripper.getText(document);

            if (extractedText == null || extractedText.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "ERROR",
                    "message", "No extractable text found in PDF (scanned image PDFs require OCR)"
                ));
            }

            int pages = document.getNumberOfPages();
            int chunksIngested = knowledgeGraphService.ingestDocument(source, extractedText);

            return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "source", source,
                "pagesProcessed", pages,
                "chunksIngested", chunksIngested,
                "message", "Successfully parsed and ingested PDF into Neo4j vector and graph store"
            ));

        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "ERROR",
                "message", "Failed to parse PDF document: " + e.getMessage()
            ));
        }
    }
}