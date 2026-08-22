package com.gii.api.model.response.student;

import com.gii.common.enums.SectionItemType;
import java.util.UUID;
import lombok.Builder;

@Builder
public record StudentSectionItemResponse(
    UUID itemId,
    SectionItemType itemType,
    Integer position,
    StudentLessonHomeResponse lesson,
    StudentQuizHomeResponse quiz,
    StudentLiveClassHomeResponse liveClass) {}
