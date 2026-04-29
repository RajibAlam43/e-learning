package com.gii.common.entity.quiz;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gii.common.entity.course.Course;
import com.gii.common.entity.course.Lesson;
import com.gii.common.entity.common.BaseUuidEntity;
import com.gii.common.enums.PublishStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "quizzes")
public class Quiz extends BaseUuidEntity {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id")
    private Lesson lesson;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "passing_score_pct", nullable = false)
    @Builder.Default
    private Integer passingScorePct = 60;

    @Column(name = "max_attempts", nullable = false)
    @Builder.Default
    private Integer maxAttempts = 3;

    @Column(name = "time_limit_sec")
    private Integer timeLimitSec;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private PublishStatus status = PublishStatus.DRAFT;
}