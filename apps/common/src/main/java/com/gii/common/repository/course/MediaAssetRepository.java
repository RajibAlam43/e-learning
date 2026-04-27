package com.gii.common.repository.course;

import com.gii.common.entity.course.MediaAsset;
import com.gii.common.enums.MediaProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MediaAssetRepository extends JpaRepository<MediaAsset, UUID> {

    Optional<MediaAsset> findByLessonId(UUID lessonId);

    boolean existsByLessonId(UUID lessonId);

    Optional<MediaAsset> findByProviderAndProviderAssetId(
            MediaProvider provider,
            String providerAssetId
    );

    boolean existsByProviderAndProviderAssetId(
            MediaProvider provider,
            String providerAssetId
    );

    Optional<MediaAsset> findByProviderAndPlaybackId(
            MediaProvider provider,
            String playbackId
    );

    boolean existsByProviderAndPlaybackId(
            MediaProvider provider,
            String playbackId
    );
}