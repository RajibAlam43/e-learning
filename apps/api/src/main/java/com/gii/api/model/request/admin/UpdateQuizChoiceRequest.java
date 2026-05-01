package com.gii.api.model.request.admin;

import java.util.UUID;
import lombok.Builder;

@Builder
public record UpdateQuizChoiceRequest(UUID choiceId, String choiceText, Boolean isCorrect) {}
