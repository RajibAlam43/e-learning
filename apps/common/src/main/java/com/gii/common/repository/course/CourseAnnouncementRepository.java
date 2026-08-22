package com.gii.common.repository.course;

import com.gii.common.entity.course.CourseAnnouncement;
import com.gii.common.enums.EnrollmentStatus;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseAnnouncementRepository extends JpaRepository<CourseAnnouncement, UUID> {

  @Query(
      value =
          """
            SELECT announcement
            FROM CourseAnnouncement announcement
            JOIN FETCH announcement.course
            WHERE EXISTS (
              SELECT enrollment.id
              FROM Enrollment enrollment
              WHERE enrollment.course = announcement.course
                AND enrollment.user.id = :userId
                AND enrollment.status = :status
                AND (enrollment.expiresAt IS NULL OR enrollment.expiresAt > :now)
            )
          """,
      countQuery =
          """
            SELECT COUNT(announcement)
            FROM CourseAnnouncement announcement
            WHERE EXISTS (
              SELECT enrollment.id
              FROM Enrollment enrollment
              WHERE enrollment.course = announcement.course
                AND enrollment.user.id = :userId
                AND enrollment.status = :status
                AND (enrollment.expiresAt IS NULL OR enrollment.expiresAt > :now)
            )
          """)
  Page<CourseAnnouncement> findForStudent(
      @Param("userId") UUID userId,
      @Param("status") EnrollmentStatus status,
      @Param("now") Instant now,
      Pageable pageable);
}
