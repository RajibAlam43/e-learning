package com.gii.api.model.response.admin;

import java.util.UUID;
import lombok.Builder;

@Builder
public record AdminQuizChoiceResponse(UUID choiceId, String choiceText, Boolean isCorrect) {}
