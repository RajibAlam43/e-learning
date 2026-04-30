package com.gii.api.model.response;

import java.util.UUID;
import lombok.Builder;

@Builder
public record InstructorSummaryResponse(
    UUID id, String fullName, String avatarUrl, String shortBio, String credentials) {}
