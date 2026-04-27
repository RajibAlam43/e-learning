package com.gii.common.entity.enrollment;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gii.common.entity.course.Lesson;
import com.gii.common.entity.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Builder
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Table(name = "lesson_progress")
public class LessonProgress {

    @EmbeddedId
    @Builder.Default
    private LessonProgressId id = LessonProgressId.builder().build();

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("lessonId")
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @Column(name = "last_position_sec")
    private Integer lastPositionSec;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;
}