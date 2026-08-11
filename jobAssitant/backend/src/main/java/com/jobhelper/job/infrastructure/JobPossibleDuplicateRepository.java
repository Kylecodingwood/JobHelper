package com.jobhelper.job.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JobPossibleDuplicateRepository extends JpaRepository<JobPossibleDuplicateEntity, UUID> {
    List<JobPossibleDuplicateEntity> findByStatusOrderByCreatedAtDesc(String status);

    Optional<JobPossibleDuplicateEntity> findByLeftJobIdAndRightJobId(UUID leftJobId, UUID rightJobId);

    @Query("select d from JobPossibleDuplicateEntity d "
            + "where d.leftJobId = :jobId or d.rightJobId = :jobId "
            + "order by d.createdAt desc")
    List<JobPossibleDuplicateEntity> findByJobId(@Param("jobId") UUID jobId);

    @Query("select d from JobPossibleDuplicateEntity d "
            + "where d.status = :status and (d.leftJobId = :jobId or d.rightJobId = :jobId) "
            + "order by d.createdAt desc")
    List<JobPossibleDuplicateEntity> findByStatusAndJobId(@Param("status") String status, @Param("jobId") UUID jobId);

    @Query("select count(d) > 0 from JobPossibleDuplicateEntity d "
            + "where d.status = 'PENDING' and (d.leftJobId = :jobId or d.rightJobId = :jobId)")
    boolean existsPendingForJob(@Param("jobId") UUID jobId);
}
