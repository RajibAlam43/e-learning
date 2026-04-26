package com.gii.api.model.request;

import lombok.Builder;

@Builder
public record LoginRequest(
        String email,
        String password
) {}
