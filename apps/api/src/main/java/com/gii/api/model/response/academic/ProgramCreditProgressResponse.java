package com.gii.api.model.response.academic;

public record ProgramCreditProgressResponse(
    int requiredCredits,
    long earnedCredits,
    long electiveCreditsCounted,
    int electiveCreditLimit,
    int mandatoryCoursesCompleted,
    int mandatoryCoursesRequired,
    boolean complete) {
  /** Mandatory requirements and credit totals must both reach 100 percent. */
  public double completionPercentage() {
    double credits = Math.min(1.0, (double) earnedCredits / requiredCredits);
    double mandatory =
        mandatoryCoursesRequired == 0
            ? 1.0
            : (double) mandatoryCoursesCompleted / mandatoryCoursesRequired;
    return Math.round(10000.0 * Math.min(credits, mandatory)) / 100.0;
  }
}
