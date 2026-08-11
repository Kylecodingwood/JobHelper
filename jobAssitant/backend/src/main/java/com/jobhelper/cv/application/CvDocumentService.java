package com.jobhelper.cv.application;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.jobhelper.cv.infrastructure.CvDocumentEntity;
import com.jobhelper.cv.infrastructure.CvDocumentRepository;
import com.jobhelper.shared.web.ApiException;

@Service
public class CvDocumentService {

    private final CvDocumentRepository repository;

    @Value("${jobhelper.cv.dir:./data/cv}")
    private String cvDir;

    @Value("${jobhelper.cv.max-bytes:10485760}")
    private long maxBytes;

    public CvDocumentService(CvDocumentRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> list() {
        List<Map<String, Object>> items = repository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toDto)
                .toList();
        return Map.of("items", items);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> get(UUID id) {
        return toDto(load(id));
    }

    @Transactional
    public Map<String, Object> upload(MultipartFile file, String displayName) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "file required");
        }
        if (file.getSize() > maxBytes) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CV_TOO_LARGE", "File exceeds 10MB");
        }
        String original = file.getOriginalFilename() == null ? "upload.bin" : file.getOriginalFilename();
        String lower = original.toLowerCase(Locale.ROOT);
        boolean isPdf = lower.endsWith(".pdf");
        boolean isDocx = lower.endsWith(".docx");
        if (!isPdf && !isDocx) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CV_INVALID_TYPE", "Only .pdf or .docx uploads are accepted");
        }

        UUID id = UUID.randomUUID();
        try {
            Path root = Path.of(cvDir).toAbsolutePath().normalize();
            Files.createDirectories(root);
            Instant now = Instant.now();
            CvDocumentEntity doc = new CvDocumentEntity();
            doc.setDocumentId(id);
            doc.setDisplayName(displayName != null && !displayName.isBlank() ? displayName.trim() : original);
            doc.setOriginalFilename(original);
            doc.setSizeBytesOriginal(file.getSize());
            doc.setCreatedAt(now);
            doc.setUpdatedAt(now);

            if (isPdf) {
                Path pdfPath = root.resolve(id + ".pdf");
                file.transferTo(pdfPath);
                if (!looksLikePdf(pdfPath)) {
                    Files.deleteIfExists(pdfPath);
                    throw new ApiException(HttpStatus.BAD_REQUEST, "CV_INVALID_TYPE", "File is not a valid PDF");
                }
                doc.setOriginalPath(pdfPath.toString());
                doc.setPdfPath(pdfPath.toString());
                doc.setContentTypeOriginal("application/pdf");
                doc.setSizeBytesPdf(Files.size(pdfPath));
                doc.setPdfReady(true);
            } else {
                // DOCX: store as-is; in-app preview is frontend-rendered (no server PDF conversion).
                Path docxPath = root.resolve(id + ".docx");
                file.transferTo(docxPath);
                doc.setOriginalPath(docxPath.toString());
                doc.setPdfPath(null);
                doc.setContentTypeOriginal("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
                doc.setSizeBytesPdf(0);
                doc.setPdfReady(false);
            }

            repository.save(doc);
            return toDto(doc);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "CV_UPLOAD_FAILED",
                    "CV upload failed: " + e.getMessage());
        }
    }

    private boolean looksLikePdf(Path path) throws Exception {
        byte[] header = new byte[5];
        try (InputStream in = Files.newInputStream(path)) {
            int n = in.read(header);
            return n >= 5 && header[0] == '%' && header[1] == 'P' && header[2] == 'D' && header[3] == 'F';
        }
    }

    @Transactional(readOnly = true)
    public Resource pdfResource(UUID id) {
        CvDocumentEntity doc = load(id);
        if (!doc.isPdfReady() || doc.getPdfPath() == null || doc.getPdfPath().isBlank()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "CV_PDF_NOT_READY",
                    "No PDF for this document — upload a .pdf for preview");
        }
        Path path = Path.of(doc.getPdfPath());
        if (!Files.exists(path)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "CV_NOT_FOUND", "PDF file missing on disk");
        }
        return new FileSystemResource(path);
    }

    /** Original uploaded file (PDF or DOCX) for open/download and in-browser DOCX preview. */
    @Transactional(readOnly = true)
    public Map<String, Object> originalFile(UUID id) {
        CvDocumentEntity doc = load(id);
        Path path = Path.of(doc.getOriginalPath());
        if (!Files.exists(path)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "CV_NOT_FOUND", "Original file missing on disk");
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("resource", new FileSystemResource(path));
        out.put("filename", doc.getOriginalFilename());
        out.put("contentType", doc.getContentTypeOriginal());
        out.put("fileKind", fileKind(doc));
        return out;
    }

    @Transactional
    public void delete(UUID id) {
        CvDocumentEntity doc = load(id);
        try {
            if (doc.getOriginalPath() != null) {
                Files.deleteIfExists(Path.of(doc.getOriginalPath()));
            }
            if (doc.getPdfPath() != null && !doc.getPdfPath().equals(doc.getOriginalPath())) {
                Files.deleteIfExists(Path.of(doc.getPdfPath()));
            }
        } catch (Exception ignored) {
            // best-effort file cleanup
        }
        repository.delete(doc);
    }

    private CvDocumentEntity load(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CV_NOT_FOUND", "CV document not found"));
    }

    private Map<String, Object> toDto(CvDocumentEntity d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("documentId", d.getDocumentId());
        m.put("displayName", d.getDisplayName());
        m.put("originalFilename", d.getOriginalFilename());
        m.put("contentTypeOriginal", d.getContentTypeOriginal());
        m.put("fileKind", fileKind(d));
        m.put("pdfReady", d.isPdfReady());
        m.put("sizeBytesOriginal", d.getSizeBytesOriginal());
        m.put("sizeBytesPdf", d.getSizeBytesPdf());
        m.put("createdAt", d.getCreatedAt());
        m.put("updatedAt", d.getUpdatedAt());
        m.put("pdfUrl", d.isPdfReady() ? "/api/v1/cv/documents/" + d.getDocumentId() + "/pdf" : null);
        m.put("fileUrl", "/api/v1/cv/documents/" + d.getDocumentId() + "/file");
        return m;
    }

    private String fileKind(CvDocumentEntity d) {
        String name = d.getOriginalFilename() == null ? "" : d.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (name.endsWith(".pdf") || "application/pdf".equals(d.getContentTypeOriginal())) {
            return "PDF";
        }
        if (name.endsWith(".docx")) {
            return "DOCX";
        }
        return "OTHER";
    }
}
