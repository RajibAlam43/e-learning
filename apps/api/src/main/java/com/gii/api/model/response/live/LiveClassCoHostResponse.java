package com.gii.api.model.response.live;

import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record LiveClassCoHostResponse(
    UUID liveClassId, String email, String role, List<String> coHostEmails, String joinUrl) {}
