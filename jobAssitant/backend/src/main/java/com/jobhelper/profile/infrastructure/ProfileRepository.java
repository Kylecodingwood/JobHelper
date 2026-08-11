package com.jobhelper.profile.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProfileRepository extends JpaRepository<ProfileEntity, UUID> {
    @Query("select p from ProfileEntity p")
    Optional<ProfileEntity> findSingleton();
}
