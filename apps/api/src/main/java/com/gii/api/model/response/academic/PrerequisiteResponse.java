package com.gii.api.model.response.academic;

import java.util.List;
import java.util.UUID;

public record PrerequisiteResponse(UUID courseTemplateId, List<CourseOption> requiredCourses) {
  public record CourseOption(UUID courseTemplateId, String title, int credits) {}
}
