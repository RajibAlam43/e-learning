package com.gii.api.service.pub;

import com.gii.common.entity.course.Course;
import com.gii.common.entity.course.CourseCategory;
import com.gii.common.enums.CourseLanguage;
import com.gii.common.enums.CourseLevel;
import com.gii.common.enums.PublishStatus;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.domain.Specification;

public class CourseSpecifications {

  public static Specification<@NotNull Course> hasStatus(PublishStatus status) {
    return (root, query, cb) -> cb.equal(root.get("status"), status);
  }

  public static Specification<@NotNull Course> hasCategory(UUID categoryId) {
    return (root, query, cb) -> {
      if (categoryId == null) {
        return null;
      }

      var subquery = query.subquery(UUID.class);
      var courseCategory = subquery.from(CourseCategory.class);

      subquery
          .select(courseCategory.get("course").get("id"))
          .where(
              cb.equal(courseCategory.get("course").get("id"), root.get("id")),
              cb.equal(courseCategory.get("category").get("id"), categoryId));

      return cb.exists(subquery);
    };
  }

  public static Specification<@NotNull Course> hasLevel(CourseLevel level) {
    return (root, query, cb) -> level == null ? null : cb.equal(root.get("level"), level);
  }

  public static Specification<@NotNull Course> hasLanguage(CourseLanguage language) {
    return (root, query, cb) -> language == null ? null : cb.equal(root.get("language"), language);
  }
}
