package com.gii.common.entity.course;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gii.common.entity.user.User;
import jakarta.persistence.*;
import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Table(name = "course_instructors")
public class CourseInstructor {

    @EmbeddedId
    @Builder.Default
    private CourseInstructorId id = CourseInstructorId.builder().build();

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("courseId")
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("instructorUserId")
    @JoinColumn(name = "instructor_user_id", nullable = false)
    private User instructor;

    @Column(name = "role", nullable = false, length = 50)
    @Builder.Default
    private String role = "primary";
}