package com.gii.common.entity.enrollment;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gii.common.entity.course.Lesson;
import com.gii.common.entity.user.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Builder
@Getter
@Setter
@Entity
@Table(name = "lesson_progress")
public class LessonProgress {

    @EmbeddedId
    private LessonProgressId id = new LessonProgressId();

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

    @Column(name = "completed", nullable = false)
    private Boolean completed = false;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "last_position_sec")
    private Integer lastPositionSec;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();

        if (Boolean.TRUE.equals(this.completed) && this.completedAt == null) {
            this.completedAt = Instant.now();
        }

        if (!Boolean.TRUE.equals(this.completed)) {
            this.completedAt = null;
        }
    }
}