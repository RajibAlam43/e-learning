package com.gii.common.entity.quiz;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gii.common.entity.course.Course;
import com.gii.common.entity.course.Lesson;
import com.gii.common.entity.common.BaseUuidEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
@Setter
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

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "passing_score_pct", nullable = false)
    private Integer passingScorePct = 60;

    @Column(name = "max_attempts", nullable = false)
    private Integer maxAttempts = 3;

    @Column(name = "time_limit_sec")
    private Integer timeLimitSec;

    @Column(name = "is_published", nullable = false)
    private Boolean isPublished = false;
}