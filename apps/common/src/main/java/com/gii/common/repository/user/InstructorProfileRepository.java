package com.gii.common.repository.user;

import com.gii.common.entity.user.InstructorProfile;
import com.gii.common.enums.UserStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InstructorProfileRepository extends JpaRepository<InstructorProfile, UUID> {

  boolean existsByUserId(UUID userId);

  @Query(
      """
        SELECT ip
        FROM InstructorProfile ip
        JOIN FETCH ip.user u
        WHERE ip.isPublic = true
        AND u.status = :status
        ORDER BY ip.createdAt DESC
      """)
  List<InstructorProfile> findPublicByUserStatus(@Param("status") UserStatus status);

  @Query(
      """
        SELECT ip
        FROM InstructorProfile ip
        JOIN FETCH ip.user u
        WHERE ip.userId = :userId
        AND ip.isPublic = true
        AND u.status = :status
      """)
  Optional<InstructorProfile> findPublicByUserIdAndStatus(
      @Param("userId") UUID userId, @Param("status") UserStatus status);
}
