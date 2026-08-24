package com.gii.common.repository.course;

import com.gii.common.entity.course.Course;
import com.gii.common.enums.PublishStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseRepository
    extends JpaRepository<Course, UUID>, JpaSpecificationExecutor<Course> {

  Optional<Course> findBySlugAndStatus(String slug, PublishStatus status);

  Page<Course> findByStatusAndIsFeaturedTrue(PublishStatus status, Pageable pageable);

  long countByStatus(PublishStatus status);

  List<Course> findByTemplateId(UUID templateId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT c FROM Course c WHERE c.id = :id")
  Optional<Course> findByIdForUpdate(@Param("id") UUID id);
}
