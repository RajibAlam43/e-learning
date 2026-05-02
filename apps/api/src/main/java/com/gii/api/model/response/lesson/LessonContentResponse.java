package com.gii.api.model.response.lesson;

import com.gii.common.enums.LessonType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record LessonContentResponse(
    UUID lessonId,
    String title,
    String slug,
    Integer position,
    LessonType lessonType,
    String description, // Optional lesson description/notes
    Integer durationSeconds, // Optional: video duration
    String thumbnailUrl, // Optional: lesson thumbnail
    String transcriptUrl, // Optional: video transcript URL
    Boolean isFree, // Whether lesson is free or requires enrollment
    Boolean isMandatory, // Whether lesson is mandatory for completion

    // Access and drip content info
    Boolean isAccessible, // Whether user can access this lesson now
    String accessReason, // e.g., "ENROLLED", "FREE", "RELEASE_PENDING", "EXPIRED"
    Instant releaseAt, // When lesson becomes available (if applicable)
    Integer unlockAfterDays, // Days after enrollment to unlock (if applicable)

    // Progress info
    LessonProgressResponse userProgress, // User's progress on this lesson

    // Related content
    MediaPlaybackResponse mediaPlayback, // Optional: media info if VIDEO/PDF type
    List<LessonResourceResponse> resources, // Lesson resources (PDFs, images)

    // Navigation context
    UUID courseId,
    UUID sectionId,
    String courseName // For breadcrumb/context
) {}
