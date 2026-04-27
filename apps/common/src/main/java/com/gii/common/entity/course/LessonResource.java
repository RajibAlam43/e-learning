package com.gii.common.entity.course;

import com.gii.common.entity.common.BaseUuidEntity;
import com.gii.common.enums.LessonResourceType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "lesson_resources",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_lesson_resources_lesson_position", columnNames = {"lesson_id", "position"})
        }
)
public class LessonResource extends BaseUuidEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 20)
    private LessonResourceType resourceType;

    @Column(name = "title")
    private String title;

    @Column(name = "file_url", nullable = false)
    private String fileUrl;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "position", nullable = false)
    private Integer position;
}
