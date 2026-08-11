package com.jobhelper.cv.api;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.jobhelper.cv.application.CvDocumentService;

@RestController
@RequestMapping("/api/v1/cv/documents")
public class CvController {
    private final CvDocumentService service;

    public CvController(CvDocumentService service) {
        this.service = service;
    }

    @GetMapping
    public Map<String, Object> list() {
        return service.list();
    }

    @GetMapping("/{documentId}")
    public Map<String, Object> get(@PathVariable UUID documentId) {
        return service.get(documentId);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "displayName", required = false) String displayName) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.upload(file, displayName));
    }

    @GetMapping("/{documentId}/pdf")
    public ResponseEntity<Resource> pdf(@PathVariable UUID documentId) {
        Resource resource = service.pdfResource(documentId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"cv-" + documentId + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }

    /** Open/download original upload (PDF or DOCX). PDF: inline; DOCX: attachment for system open. */
    @GetMapping("/{documentId}/file")
    public ResponseEntity<Resource> file(@PathVariable UUID documentId) {
        Map<String, Object> payload = service.originalFile(documentId);
        Resource resource = (Resource) payload.get("resource");
        String filename = (String) payload.get("filename");
        String contentType = (String) payload.get("contentType");
        boolean pdf = "PDF".equals(payload.get("fileKind"));
        ContentDisposition disposition = (pdf ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(filename == null ? "cv.bin" : filename, StandardCharsets.UTF_8)
                .build();
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(contentType == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : contentType);
        } catch (Exception e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(mediaType)
                .body(resource);
    }

    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> delete(@PathVariable UUID documentId) {
        service.delete(documentId);
        return ResponseEntity.noContent().build();
    }
}
