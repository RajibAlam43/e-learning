package com.gii.api.processor;

import com.gii.api.model.response.CategoryResponse;
import com.gii.api.service.open.AllCategoriesService;
import com.gii.api.service.open.AllCoursesService;
import com.gii.api.service.open.CourseDetailsService;
import com.gii.api.model.response.CourseDetailsResponse;
import com.gii.api.model.response.CourseSummaryResponse;
import com.gii.api.model.response.PageResponse;
import com.gii.common.enums.CourseLanguage;
import com.gii.common.enums.CourseLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PublicApiProcessingService {

    private final AllCoursesService allCoursesService;
    private final CourseDetailsService courseDetailsService;
    private final AllCategoriesService allCategoriesService;

    public PageResponse<CourseSummaryResponse> getAllCourses(UUID categoryId, CourseLevel level, CourseLanguage language, Pageable pageable) {
        return allCoursesService.execute(categoryId, level, language, pageable);
    }

    public CourseDetailsResponse getCourseDetails(String slug) {
        return courseDetailsService.execute(slug);
    }

    public List<CategoryResponse> getCategories() {
        return allCategoriesService.execute();
    }
}
