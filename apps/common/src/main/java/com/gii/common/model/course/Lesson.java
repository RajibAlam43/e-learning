package com.gii.common.model.course;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gii.common.model.common.BaseUuidEntity;
import com.gii.common.model.enums.LessonStatus;
import com.gii.common.model.enums.LessonType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "lessons",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_lessons_course_slug", columnNames = {"course_id", "slug"}),
                @UniqueConstraint(name = "uk_lessons_course_position", columnNames = {"course_id", "position"})
        }
)
public class Lesson extends BaseUuidEntity {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id")
    private CourseSection section;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "slug", nullable = false, length = 255)
    private String slug;

    @Column(name = "position", nullable = false)
    private Integer position;

    @Enumerated(EnumType.STRING)
    @Column(name = "lesson_type", nullable = false, length = 30)
    private LessonType lessonType = LessonType.video;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_asset_id")
    private MediaAsset mediaAsset;

    @Column(name = "is_preview_free", nullable = false)
    private Boolean isPreviewFree = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private LessonStatus status = LessonStatus.draft;
}