package com.gii.common.model.course;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gii.common.model.common.CreatedOnlyUuidEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "course_sections",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_course_sections_course_position", columnNames = {"course_id", "position"})
        }
)
public class CourseSection extends CreatedOnlyUuidEntity {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "position", nullable = false)
    private Integer position;
}