package com.gii.api.model.response.student;

import lombok.Builder;

@Builder
public record StudentCompletedLessonsResponse(Long totalCompletedLessons) {}
