package com.gii.common.entity.course;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gii.common.entity.common.BaseUuidEntity;
import com.gii.common.entity.user.User;
import com.gii.common.enums.*;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "media_assets",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_media_lesson",
                        columnNames = {"lesson_id"}
                )
        }
)
public class MediaAsset extends BaseUuidEntity {

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 30)
    private MediaProvider provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, length = 30)
    @Builder.Default
    private MediaAssetType assetType = MediaAssetType.VIDEO;

    @Column(name = "provider_asset_id")
    private String providerAssetId;

    @Column(name = "provider_library_id")
    private String providerLibraryId;

    @Column(name = "playback_id")
    private String playbackId;

    @Enumerated(EnumType.STRING)
    @Column(name = "playback_policy", length = 30)
    private PlaybackPolicy playbackPolicy;

    @Column(name = "file_url")
    private String fileUrl;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "max_resolution", length = 20)
    private String maxResolution;

    @Column(name = "duration_sec")
    private Integer durationSec;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private MediaStatus status = MediaStatus.READY;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_playback_mode", length = 30)
    private PlaybackMode preferredPlaybackMode;
}