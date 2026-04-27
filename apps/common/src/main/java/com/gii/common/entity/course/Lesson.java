package com.gii.common.entity.course;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gii.common.entity.common.BaseUuidEntity;
import com.gii.common.enums.LessonType;
import com.gii.common.enums.PublishStatus;
import com.gii.common.enums.ReleaseType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@SuperBuilder
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "lessons",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_lessons_course_slug", columnNames = {"course_id", "slug"}),
                @UniqueConstraint(name = "uk_lessons_section_position", columnNames = {"section_id", "position"})
        }
)
public class Lesson extends BaseUuidEntity {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_id", nullable = false)
    private CourseSection section;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "slug", nullable = false)
    private String slug;

    @Column(name = "position", nullable = false)
    private Integer position;

    @Enumerated(EnumType.STRING)
    @Column(name = "lesson_type", nullable = false, length = 30)
    @Builder.Default
    private LessonType lessonType = LessonType.VIDEO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primary_media_asset_id")
    private MediaAsset primaryMediaAsset;


    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private PublishStatus status = PublishStatus.DRAFT;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "transcript_url")
    private String transcriptUrl;

    // Drip content fields
    @Column(name = "is_free", nullable = false)
    @Builder.Default
    private Boolean isFree = false;

    @Column(name = "is_mandatory", nullable = false)
    @Builder.Default
    private Boolean isMandatory = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "release_type", length = 30)
    private ReleaseType releaseType;

    @Column(name = "release_at")
    private Instant releaseAt;

    @Column(name = "unlock_after_days")
    private Integer unlockAfterDays;
}
