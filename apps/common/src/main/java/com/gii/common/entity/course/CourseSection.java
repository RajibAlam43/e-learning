package com.gii.common.entity.course;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gii.common.entity.common.BaseUuidEntity;
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
        name = "course_sections",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_course_sections_course_position", columnNames = {"course_id", "position"}),
                @UniqueConstraint(name = "uk_course_sections_course_slug", columnNames = {"course_id", "slug"})
        }
)
public class CourseSection extends BaseUuidEntity {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "slug", nullable = false)
    private String slug;

    @Column(name = "position", nullable = false)
    private Integer position;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private PublishStatus status = PublishStatus.DRAFT;

    @Column(name = "published_at")
    private Instant publishedAt;

    // Drip Content Fields
    @Column(name = "is_mandatory", nullable = false)
    @Builder.Default
    private Boolean isMandatory = false;

    @Column(name = "is_free", nullable = false)
    @Builder.Default
    private Boolean isFree = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "release_type", nullable = false, length = 30)
    @Builder.Default
    private ReleaseType releaseType = ReleaseType.IMMEDIATE;

    @Column(name = "release_at")
    private Instant releaseAt;

    @Column(name = "unlock_after_days")
    private Integer unlockAfterDays;
}
