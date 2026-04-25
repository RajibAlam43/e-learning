package com.gii.common.model.enrollment;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gii.common.model.course.Course;
import com.gii.common.model.user.User;
import com.gii.common.model.common.CreatedOnlyUuidEntity;
import com.gii.common.model.enums.EnrollmentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(
        name = "enrollments",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_enrollments_user_course", columnNames = {"user_id", "course_id"})
        }
)
public class Enrollment extends CreatedOnlyUuidEntity {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private EnrollmentStatus status = EnrollmentStatus.active;

    @Column(name = "enrolled_at", nullable = false)
    private Instant enrolledAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @PrePersist
    protected void onCreateEnrollment() {
        super.onCreate();
        if (this.enrolledAt == null) {
            this.enrolledAt = Instant.now();
        }
    }
}