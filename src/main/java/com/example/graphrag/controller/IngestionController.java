package com.example.graphrag.controller;

import com.example.graphrag.service.KnowledgeGraphService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/ingest")
public class IngestionController {

    private final KnowledgeGraphService knowledgeGraphService;

    public IngestionController(KnowledgeGraphService knowledgeGraphService) {
        this.knowledgeGraphService = knowledgeGraphService;
    }

    /**
     * Reactive text ingestion endpoint.
     * Consumes JSON and returns Mono<ResponseEntity<Map<String, Object>>>.
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> ingestText(@RequestBody Mono<Map<String, String>> requestMono) {
        return requestMono
                .flatMap(request -> {
                    String source = request.getOrDefault("source", "raw-text-doc");
                    String content = request.get("content");

                    if (content == null || content.isBlank()) {
                        return Mono.just(ResponseEntity.badRequest().body(Map.<String, Object>of(
                            "status", "ERROR",
                            "message", "'content' field is required"
                        )));
                    }

                    return knowledgeGraphService.ingestDocumentReactive(source, content)
                            .map(chunksIngested -> ResponseEntity.ok(Map.<String, Object>of(
                                "status", "SUCCESS",
                                "source", source,
                                "chunksIngested", chunksIngested,
                                "message", "Successfully ingested text into Neo4j vector store and knowledge graph"
                            )));
                })
                .defaultIfEmpty(ResponseEntity.badRequest().body(Map.of(
                    "status", "ERROR",
                    "message", "Request body cannot be empty"
                )));
    }

    /**
     * Reactive PDF document upload endpoint.
     * Streams FilePart data buffers non-blockingly, extracts text, and ingests into Neo4j.
     */
    @PostMapping(value = "/pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> ingestPdf(
            @RequestPart("file") Mono<FilePart> filePartMono,
            @RequestPart(value = "source", required = false) Mono<String> customSourceMono) {

        return Mono.zip(filePartMono, customSourceMono.defaultIfEmpty(""))
                .flatMap(tuple -> {
                    FilePart filePart = tuple.getT1();
                    String customSource = tuple.getT2();
                    String filename = filePart.filename();

                    if (!filename.toLowerCase().endsWith(".pdf")) {
                        return Mono.just(ResponseEntity.badRequest().body(Map.<String, Object>of(
                            "status", "ERROR",
                            "message", "File must have a .pdf extension"
                        )));
                    }

                    String source = (!customSource.isBlank()) ? customSource : filename;

                    // Stream DataBuffers reactively into a single byte array
                    return DataBufferUtils.join(filePart.content())
                            .map(dataBuffer -> {
                                byte[] bytes = new byte[dataBuffer.readableByteCount()];
                                dataBuffer.read(bytes);
                                DataBufferUtils.release(dataBuffer);
                                return bytes;
                            })
                            .publishOn(Schedulers.boundedElastic())
                            .flatMap(bytes -> extractAndIngestPdf(bytes, source));
                });
    }

    private Mono<ResponseEntity<Map<String, Object>>> extractAndIngestPdf(byte[] pdfBytes, String source) {
        return Mono.fromCallable(() -> {
            try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdfBytes))) {
                if (document.isEncrypted()) {
                    throw new IllegalArgumentException("Encrypted/Password-protected PDF files are not supported");
                }

                PDFTextStripper textStripper = new PDFTextStripper();
                String extractedText = textStripper.getText(document);

                if (extractedText == null || extractedText.isBlank()) {
                    throw new IllegalArgumentException("No extractable text found in PDF (scanned image PDFs require OCR)");
                }

                int pages = document.getNumberOfPages();
                return Map.entry(pages, extractedText);
            }
        })
        .subscribeOn(Schedulers.boundedElastic())
        .flatMap(entry -> {
            int pages = entry.getKey();
            String text = entry.getValue();

            return knowledgeGraphService.ingestDocumentReactive(source, text)
                    .map(chunksIngested -> ResponseEntity.ok(Map.<String, Object>of(
                        "status", "SUCCESS",
                        "source", source,
                        "pagesProcessed", pages,
                        "chunksIngested", chunksIngested,
                        "message", "Successfully parsed and ingested PDF into Neo4j vector and graph store"
                    )));
        })
        .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().body(Map.of(
            "status", "ERROR",
            "message", e.getMessage()
        ))));
    }
}