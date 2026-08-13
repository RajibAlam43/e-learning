package com.gii.common.repository.course;

import com.gii.common.entity.course.CourseReview;
import com.gii.common.enums.ReviewStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseReviewRepository extends JpaRepository<CourseReview, UUID> {

  boolean existsByCourseIdAndUserId(UUID courseId, UUID userId);

  Optional<CourseReview> findByCourseIdAndUserId(UUID courseId, UUID userId);

  @Query(
      """
        SELECT r FROM CourseReview r
        JOIN FETCH r.user
        WHERE r.course.id = :courseId AND r.status = :status
        ORDER BY r.createdAt DESC
      """)
  List<CourseReview> findByCourseIdAndStatus(
      @Param("courseId") UUID courseId, @Param("status") ReviewStatus status);

  @Query(
      """
        SELECT r FROM CourseReview r
        JOIN FETCH r.course
        JOIN FETCH r.user
        WHERE (:status IS NULL OR r.status = :status)
        ORDER BY r.createdAt DESC
      """)
  List<CourseReview> findAllForAdmin(@Param("status") ReviewStatus status);

  @Query(
      """
        SELECT r.course.id, AVG(r.rating), COUNT(r)
        FROM CourseReview r
        WHERE r.course.id IN :courseIds AND r.status = :status
        GROUP BY r.course.id
      """)
  List<Object[]> aggregateByCourseIdsAndStatus(
      @Param("courseIds") List<UUID> courseIds, @Param("status") ReviewStatus status);

  @Query(
      """
        SELECT AVG(r.rating), COUNT(r)
        FROM CourseReview r
        WHERE r.course.id = :courseId AND r.status = :status
      """)
  List<Object[]> aggregateByCourseIdAndStatus(
      @Param("courseId") UUID courseId, @Param("status") ReviewStatus status);
}
