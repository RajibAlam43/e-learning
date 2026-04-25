package com.gii.common.model.course;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gii.common.model.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "course_instructors")
public class CourseInstructor {

    @EmbeddedId
    private CourseInstructorId id = new CourseInstructorId();

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
    private String role = "primary";
}