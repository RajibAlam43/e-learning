package com.gii.common.entity.course;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gii.common.entity.common.BaseUuidEntity;
import com.gii.common.enums.CourseLanguage;
import com.gii.common.enums.CourseLevel;
import com.gii.common.entity.user.User;
import com.gii.common.enums.PublishStatus;
import com.gii.common.enums.StudyMode;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@SuperBuilder
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "courses")
public class Course extends BaseUuidEntity {

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "slug", nullable = false, unique = true)
    private String slug;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "short_description")
    private String shortDescription;

    @Column(name = "description")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "highlights", columnDefinition = "jsonb")
    private List<String> highlights;

    @Column(name = "price_bdt", nullable = false)
    @Builder.Default
    private BigDecimal priceBdt = BigDecimal.ZERO;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "course_outcomes", columnDefinition = "jsonb")
    private List<String> courseOutcomes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "requirements", columnDefinition = "jsonb")
    private List<String> requirements;

    @Column(name = "prerequisites", columnDefinition = "text")
    private String prerequisites;

    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false, length = 30)
    @Builder.Default
    private CourseLevel level = CourseLevel.BEGINNER;

    @Enumerated(EnumType.STRING)
    @Column(name = "language", nullable = false, length = 20)
    @Builder.Default
    private CourseLanguage language = CourseLanguage.BN;

    @Enumerated(EnumType.STRING)
    @Column(name = "study_mode", nullable = false, length = 30)
    @Builder.Default
    private StudyMode studyMode = StudyMode.SCHEDULED;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private PublishStatus status = PublishStatus.DRAFT;

    @Column(name = "published_at")
    private Instant publishedAt;

    // For ease of use
    @Column(name = "is_free", nullable = false)
    @Builder.Default
    private Boolean isFree = false;

    @Column(name = "live_session_count", nullable = false)
    private Integer liveSessionCount;

    @Column(name = "quiz_count", nullable = false)
    private Integer quizCount;

    @Column(name = "recorded_hours_count", nullable = false)
    private Integer recordedHoursCount;

    // Not currently used
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preview_lesson_id")
    private Lesson previewLesson;

    @Column(name = "estimated_duration_minutes")
    private Integer estimatedDurationMinutes;

    @Column(name = "target_audience", columnDefinition = "text")
    private String targetAudience;
}