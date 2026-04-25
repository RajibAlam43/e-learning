package com.gii.common.model.course;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gii.common.model.common.BaseUuidEntity;
import com.gii.common.model.enums.CourseLanguage;
import com.gii.common.model.enums.CourseLevel;
import com.gii.common.model.enums.CourseStatus;
import com.gii.common.model.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "courses")
public class Course extends BaseUuidEntity {

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "slug", nullable = false, unique = true, length = 255)
    private String slug;

    @Column(name = "short_description")
    private String shortDescription;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "language", nullable = false, length = 20)
    private CourseLanguage language = CourseLanguage.bn;

    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false, length = 30)
    private CourseLevel level = CourseLevel.beginner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "price_bdt", nullable = false)
    private Integer priceBdt = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private CourseStatus status = CourseStatus.draft;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;
}