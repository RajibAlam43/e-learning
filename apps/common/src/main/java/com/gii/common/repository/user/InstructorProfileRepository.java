package com.gii.common.repository.user;

import com.gii.common.entity.user.InstructorProfile;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstructorProfileRepository extends JpaRepository<InstructorProfile, UUID> {

  boolean existsByUserId(UUID userId);
}
