package com.gii.api.model.response.lesson;

import java.util.UUID;
import lombok.Builder;

@Builder
public record CourseProgressResponse(UUID courseId, Double completionPercentage) {}
