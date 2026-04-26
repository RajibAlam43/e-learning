package com.gii.api.model.request;

import lombok.Builder;

@Builder
public record RegisterRequest(
        String fullName,
        String email,
        String phoneNumber,
        String password
) {}
