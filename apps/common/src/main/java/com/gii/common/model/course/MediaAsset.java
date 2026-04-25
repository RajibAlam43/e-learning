package com.gii.common.model.course;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gii.common.model.common.BaseUuidEntity;
import com.gii.common.model.enums.*;
import com.gii.common.model.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "media_assets")
public class MediaAsset extends BaseUuidEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 30)
    private MediaProvider provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, length = 30)
    private MediaAssetType assetType = MediaAssetType.video;

    @Column(name = "provider_asset_id")
    private String providerAssetId;

    @Column(name = "playback_id")
    private String playbackId;

    @Convert(converter = PlaybackPolicyConverter.class)
    @Column(name = "playback_policy", length = 30)
    private PlaybackPolicy playbackPolicy;

    @Column(name = "file_url")
    private String fileUrl;

    @Column(name = "duration_sec")
    private Integer durationSec;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private MediaStatus status = MediaStatus.ready;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;
}