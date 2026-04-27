package com.gii.api.controller;

import com.gii.api.model.response.CategoryResponse;
import com.gii.api.processor.PublicApiProcessingService;
import com.gii.api.model.response.CourseDetailsResponse;
import com.gii.api.model.response.CourseSummaryResponse;
import com.gii.api.model.response.PageResponse;
import com.gii.common.enums.CourseLanguage;
import com.gii.common.enums.CourseLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/public")
public class PublicApiController {

    private final PublicApiProcessingService publicApiProcessingService;

    @GetMapping("/courses")
    public ResponseEntity<PageResponse<CourseSummaryResponse>> getAllCourses(@RequestParam(required = false) UUID categoryId,
                                                                             @RequestParam(required = false) CourseLevel level,
                                                                             @RequestParam(required = false) CourseLanguage language,
                                                                             Pageable pageable) {
        return ResponseEntity.ok(publicApiProcessingService.getAllCourses(categoryId, level, language, pageable));
    }

    @GetMapping("/courses/{slug}")
    public ResponseEntity<CourseDetailsResponse> getCourseDetail(@PathVariable String slug) {
        return ResponseEntity.ok(publicApiProcessingService.getCourseDetails(slug));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryResponse>> getCategories() {
        return ResponseEntity.ok(publicApiProcessingService.getCategories());
    }
}
