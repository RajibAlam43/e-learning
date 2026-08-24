package com.gii.common.repository.course;

import com.gii.common.entity.course.MuxVideoUpload;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MuxVideoUploadRepository extends JpaRepository<MuxVideoUpload, UUID> {

  Optional<MuxVideoUpload> findByUploadId(String uploadId);

  Optional<MuxVideoUpload> findByAssetId(String assetId);

  Optional<MuxVideoUpload> findFirstByLessonIdOrderByCreatedAtDesc(UUID lessonId);
}
