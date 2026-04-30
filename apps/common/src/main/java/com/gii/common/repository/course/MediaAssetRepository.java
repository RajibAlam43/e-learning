package com.gii.common.repository.course;

import com.gii.common.entity.course.MediaAsset;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaAssetRepository extends JpaRepository<MediaAsset, UUID> {

  Optional<MediaAsset> findByLessonId(UUID lessonId);

  boolean existsByLessonId(UUID lessonId);
}
