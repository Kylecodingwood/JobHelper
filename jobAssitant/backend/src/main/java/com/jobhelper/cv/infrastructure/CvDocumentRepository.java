package com.jobhelper.cv.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CvDocumentRepository extends JpaRepository<CvDocumentEntity, UUID> {
    List<CvDocumentEntity> findAllByOrderByCreatedAtDesc();
}
