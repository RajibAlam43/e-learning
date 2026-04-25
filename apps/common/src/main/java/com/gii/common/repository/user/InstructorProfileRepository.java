package com.gii.common.repository.user;

import com.gii.common.model.user.InstructorProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InstructorProfileRepository extends JpaRepository<InstructorProfile, UUID> {

    List<InstructorProfile> findByIsPublicTrue();

    boolean existsByUserId(UUID userId);
}