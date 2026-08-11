package com.jobhelper.cv.infrastructure;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "cv_document")
public class CvDocumentEntity {
    @Id
    @Column(name = "document_id")
    private UUID documentId;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "original_path", nullable = false)
    private String originalPath;

    @Column(name = "pdf_path")
    private String pdfPath;

    @Column(name = "content_type_original", nullable = false)
    private String contentTypeOriginal;

    @Column(name = "size_bytes_original", nullable = false)
    private long sizeBytesOriginal;

    @Column(name = "size_bytes_pdf", nullable = false)
    private long sizeBytesPdf;

    @Column(name = "pdf_ready", nullable = false)
    private boolean pdfReady;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getDocumentId() { return documentId; }
    public void setDocumentId(UUID documentId) { this.documentId = documentId; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }
    public String getOriginalPath() { return originalPath; }
    public void setOriginalPath(String originalPath) { this.originalPath = originalPath; }
    public String getPdfPath() { return pdfPath; }
    public void setPdfPath(String pdfPath) { this.pdfPath = pdfPath; }
    public String getContentTypeOriginal() { return contentTypeOriginal; }
    public void setContentTypeOriginal(String contentTypeOriginal) { this.contentTypeOriginal = contentTypeOriginal; }
    public long getSizeBytesOriginal() { return sizeBytesOriginal; }
    public void setSizeBytesOriginal(long sizeBytesOriginal) { this.sizeBytesOriginal = sizeBytesOriginal; }
    public long getSizeBytesPdf() { return sizeBytesPdf; }
    public void setSizeBytesPdf(long sizeBytesPdf) { this.sizeBytesPdf = sizeBytesPdf; }
    public boolean isPdfReady() { return pdfReady; }
    public void setPdfReady(boolean pdfReady) { this.pdfReady = pdfReady; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
