package com.gii.api.model.request.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record SetCollectionCoursesRequest(@NotEmpty List<@Valid Item> items) {
  public record Item(@NotNull UUID courseId, @NotNull Integer position, Boolean isMandatory) {}
}
