package com.gii.api.controller;

import com.gii.api.model.Placeholder;
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

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/public")
public class PublicApiController {

    private final PublicApiProcessingService publicApiProcessingService;


    /**
     * List all published courses, with optional filters/search
     *
     * @param categoryId
     * @param level
     * @param language
     * @param pageable
     * @return
     */
    @GetMapping("/courses")
    public ResponseEntity<PageResponse<CourseSummaryResponse>> getAllCourses(@RequestParam(required = false) UUID categoryId,
                                                                             @RequestParam(required = false) CourseLevel level,
                                                                             @RequestParam(required = false) CourseLanguage language,
                                                                             Pageable pageable) {
        return ResponseEntity.ok(publicApiProcessingService.getAllCourses(categoryId, level, language, pageable));
    }

    /**
     * Get public course details: description, sections, lessons preview, instructor, price
     *
     * @param slug
     * @return
     */
    @GetMapping("/courses/{slug}")
    public ResponseEntity<CourseDetailsResponse> getCourseDetails(@PathVariable String slug) {
        return ResponseEntity.ok(publicApiProcessingService.getCourseDetails(slug));
    }

    /**
     * List all published instructors
     *
     * @return
     */
    @GetMapping("/instructors")
    public ResponseEntity<Placeholder> getAllInstructors() {
        return ResponseEntity.ok(new Placeholder());
    }


    /**
     * List all published instructors
     *
     * @return
     */
    @GetMapping("/instructors/{slug}")
    public ResponseEntity<Placeholder> getInstructorDetails(@PathVariable String slug) {
        return ResponseEntity.ok(new Placeholder());
    }
}
