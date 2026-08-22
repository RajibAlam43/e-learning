package com.gii.api.model.response.student;

import java.time.Instant;
import java.time.LocalDate;
import lombok.Builder;

@Builder
public record StudentLearningStreakResponse(
    Integer currentStreak, Integer maxStreak, LocalDate lastActivityDate, Instant lastActivityAt) {}
