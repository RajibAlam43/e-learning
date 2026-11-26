package com.gii.api.model.response.academic;

public record ProgramCreditProgressResponse(
    int requiredCredits,
    long earnedCredits,
    long electiveCreditsCounted,
    int electiveCreditLimit,
    int mandatoryCoursesCompleted,
    int mandatoryCoursesRequired,
    boolean complete) {}
