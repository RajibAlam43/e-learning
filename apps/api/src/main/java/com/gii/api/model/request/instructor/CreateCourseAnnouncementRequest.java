package com.gii.api.model.request.instructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCourseAnnouncementRequest(
    @NotBlank @Size(max = 200) String title, @NotBlank @Size(max = 10000) String content) {}
