package com.jobhelper.behavioral.infrastructure;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BhvQuestionRepository extends JpaRepository<BhvQuestionEntity, UUID> {
}
