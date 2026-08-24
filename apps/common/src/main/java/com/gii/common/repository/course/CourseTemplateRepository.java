package com.gii.common.repository.course;

import com.gii.common.entity.course.CourseTemplate;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseTemplateRepository extends JpaRepository<CourseTemplate, UUID> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT template FROM CourseTemplate template WHERE template.id = :id")
  Optional<CourseTemplate> findByIdForUpdate(@Param("id") UUID id);
}
