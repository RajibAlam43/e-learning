package com.gii.api.model.response;

import lombok.Builder;

@Builder
public record AuthResponse(
        String accessToken
) {}
