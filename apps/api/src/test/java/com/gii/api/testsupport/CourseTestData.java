package com.gii.api.testsupport;

import com.gii.common.entity.course.Course;
import com.gii.common.entity.course.CourseTemplate;
import com.gii.common.entity.course.CourseTemplateVersion;
import com.gii.common.entity.user.User;
import com.gii.common.enums.CourseLanguage;
import com.gii.common.enums.CourseLevel;
import com.gii.common.enums.PublishStatus;
import com.gii.common.enums.StudyMode;
import java.math.BigDecimal;

public final class CourseTestData {

  private CourseTestData() {}

  public static Course course(String title, String slug, User creator) {
    CourseTemplate template = CourseTemplate.builder().internalKey(slug).build();
    CourseTemplateVersion version =
        CourseTemplateVersion.builder()
            .courseTemplate(template)
            .versionNumber(1)
            .status(PublishStatus.DRAFT)
            .title(title)
            .level(CourseLevel.BEGINNER)
            .language(CourseLanguage.EN)
            .liveSessionCount(0)
            .quizCount(0)
            .recordedHoursCount(0)
            .build();
    return Course.builder()
        .templateVersion(version)
        .name(title)
        .slug(slug)
        .priceBdt(BigDecimal.ZERO)
        .studyMode(StudyMode.COHORT_BASED)
        .status(PublishStatus.DRAFT)
        .isFree(false)
        .createdBy(creator)
        .build();
  }
}
