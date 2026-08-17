package com.gii.api.model.request.student;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateCourseReviewRequest(
    @Min(1) @Max(5) Integer rating, @Size(max = 5000) String reviewText) {

  @AssertTrue(message = "At least one review field must be provided")
  public boolean hasUpdates() {
    return rating != null || reviewText != null;
  }

  @AssertTrue(message = "Review text must not be blank")
  public boolean isReviewTextValid() {
    return reviewText == null || !reviewText.isBlank();
  }
}
