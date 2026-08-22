package com.gii.api.model.response;

import lombok.Builder;

@Builder
public record PublicPlatformStatsResponse(
    long students, long courses, long programs, long instructors) {}
