package com.gii.api.model.response;

import com.gii.common.enums.MediaProvider;
import lombok.Builder;

@Builder
public record LessonVideoResponse(MediaProvider provider, String sourceId) {}
