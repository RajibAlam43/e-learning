package com.gii.api.service.pub;

import com.gii.api.model.response.CourseSummaryResponse;
import com.gii.api.model.response.InstructorDetailsResponse;
import com.gii.api.service.localization.LocalizedContentService;
import com.gii.api.service.storage.AssetUrlService;
import com.gii.common.entity.course.Course;
import com.gii.common.entity.course.CourseInstructor;
import com.gii.common.entity.user.InstructorProfile;
import com.gii.common.enums.PublishStatus;
import com.gii.common.enums.UserStatus;
import com.gii.common.repository.course.CourseInstructorRepository;
import com.gii.common.repository.user.InstructorProfileRepository;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InstructorDetailsService {

  private final InstructorProfileRepository instructorProfileRepository;
  private final CourseInstructorRepository courseInstructorRepository;
  private final AssetUrlService assetUrlService;
  private final LocalizedContentService localizedContentService;

  public InstructorDetailsResponse execute(String slug) {
    UUID instructorId = parseInstructorId(slug);
    InstructorProfile profile =
        instructorProfileRepository
            .findPublicByUserIdAndStatus(instructorId, UserStatus.ACTIVE)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Instructor not found"));

    List<CourseSummaryResponse> publishedCourses =
        courseInstructorRepository
            .findByInstructorIdAndCourseStatus(instructorId, PublishStatus.PUBLISHED)
            .stream()
            .map(CourseInstructor::getCourse)
            .sorted(
                Comparator.comparing(
                        Course::getPublishedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(Course::getId))
            .map(this::toCourseSummary)
            .toList();

    return InstructorDetailsResponse.builder()
        .id(profile.getUserId())
        .slug(profile.getUserId().toString())
        .fullName(profile.getUser().getFullName())
        .displayName(profile.getDisplayName())
        .avatarUrl(profile.getPhotoUrl())
        .headline(localizedContentService.text(profile.getHeadline(), profile.getHeadlineEn()))
        .institution(
            localizedContentService.text(profile.getInstitution(), profile.getInstitutionEn()))
        .expertiseArea(
            localizedContentService.text(profile.getExpertiseArea(), profile.getExpertiseAreaEn()))
        .about(localizedContentService.text(profile.getAbout(), profile.getAboutEn()))
        .credentialsText(
            localizedContentService.text(
                profile.getCredentialsText(), profile.getCredentialsTextEn()))
        .specialties(
            localizedContentService.list(profile.getSpecialties(), profile.getSpecialtiesEn()))
        .yearsExperience(profile.getYearsExperience())
        .publishedCourses(publishedCourses)
        .build();
  }

  private UUID parseInstructorId(String slug) {
    try {
      return UUID.fromString(slug);
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Instructor not found");
    }
  }

  private CourseSummaryResponse toCourseSummary(Course course) {
    return CourseSummaryResponse.builder()
        .id(course.getId())
        .title(localizedContentService.text(course.getTitle(), course.getTitleEn()))
        .slug(course.getSlug())
        .shortDescription(
            localizedContentService.text(
                course.getShortDescription(), course.getShortDescriptionEn()))
        .thumbnailUrl(assetUrlService.publicUrl(course.getThumbnailObjectKey()))
        .priceBdt(course.getPriceBdt())
        .level(course.getLevel())
        .language(course.getLanguage())
        .publishedAt(course.getPublishedAt())
        .categoryNames(List.of())
        .instructorNames(List.of())
        .build();
  }
}
