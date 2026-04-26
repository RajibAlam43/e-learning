package com.gii.api.service.open;

import com.gii.common.entity.course.Course;
import com.gii.common.enums.CourseLanguage;
import com.gii.common.enums.CourseLevel;
import com.gii.common.enums.CourseStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public class CourseSpecifications {

    public static Specification<Course> hasStatus(CourseStatus status) {
        return (root, query, cb) ->
                cb.equal(root.get("status"), status);
    }

    public static Specification<Course> hasCategory(UUID categoryId) {
        return (root, query, cb) ->
                categoryId == null ? null :
                        cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Course> hasLevel(CourseLevel level) {
        return (root, query, cb) ->
                level == null ? null :
                        cb.equal(root.get("level"), level);
    }

    public static Specification<Course> hasLanguage(CourseLanguage language) {
        return (root, query, cb) ->
                language == null ? null :
                        cb.equal(root.get("language"), language);
    }
}
