package com.jobhelper.application.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailApplicationRepository extends JpaRepository<EmailApplicationEntity, UUID> {
    Optional<EmailApplicationEntity> findByCompanyNameAndRoleName(String companyName, String roleName);
    List<EmailApplicationEntity> findAllByOrderByCategoryAscCompanyNameAsc();
}
