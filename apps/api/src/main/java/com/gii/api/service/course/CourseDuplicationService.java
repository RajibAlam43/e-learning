package com.gii.api.service.course;

import com.gii.common.entity.course.Course;
import com.gii.common.entity.course.CourseCategory;
import com.gii.common.entity.course.CourseSection;
import com.gii.common.entity.course.CourseTemplate;
import com.gii.common.entity.course.Lesson;
import com.gii.common.entity.course.LessonResource;
import com.gii.common.entity.course.MediaAsset;
import com.gii.common.entity.course.SectionItem;
import com.gii.common.entity.live.LiveClassSlot;
import com.gii.common.entity.quiz.Quiz;
import com.gii.common.entity.quiz.QuizChoice;
import com.gii.common.entity.quiz.QuizQuestion;
import com.gii.common.entity.user.User;
import com.gii.common.enums.MediaStatus;
import com.gii.common.enums.ReleaseType;
import com.gii.common.repository.course.CourseCategoryRepository;
import com.gii.common.repository.course.CourseRepository;
import com.gii.common.repository.course.CourseSectionRepository;
import com.gii.common.repository.course.CourseTemplateRepository;
import com.gii.common.repository.course.LessonRepository;
import com.gii.common.repository.course.LessonResourceRepository;
import com.gii.common.repository.course.MediaAssetRepository;
import com.gii.common.repository.course.SectionItemRepository;
import com.gii.common.repository.live.LiveClassSlotRepository;
import com.gii.common.repository.quiz.QuizChoiceRepository;
import com.gii.common.repository.quiz.QuizQuestionRepository;
import com.gii.common.repository.quiz.QuizRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Deep-clones a course's curriculum into a brand-new, independent {@link CourseTemplate} and
 * attaches a new {@link Course} offering to it. The clone is fully independent of the source —
 * editing one never leaks into the other.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CourseDuplicationService {

  private final CourseTemplateRepository templateRepository;
  private final CourseRepository courseRepository;
  private final CourseCategoryRepository categoryRepository;
  private final CourseSectionRepository sectionRepository;
  private final LessonRepository lessonRepository;
  private final LessonResourceRepository resourceRepository;
  private final MediaAssetRepository mediaAssetRepository;
  private final QuizRepository quizRepository;
  private final QuizQuestionRepository questionRepository;
  private final QuizChoiceRepository choiceRepository;
  private final LiveClassSlotRepository liveClassSlotRepository;
  private final SectionItemRepository sectionItemRepository;

  public Course duplicate(Course source, String slug, User createdBy) {
    CourseTemplate target = templateRepository.save(copyTemplate(source.getTemplate()));
    copyCategories(source, target);

    Map<UUID, UUID> itemIds = new HashMap<>();
    Map<UUID, Lesson> lessons = new HashMap<>();
    for (CourseSection sourceSection :
        sectionRepository.findByCourseIdOrderByPositionAsc(source.getId())) {
      CourseSection targetSection = sectionRepository.save(copySection(sourceSection, target));
      copyLessons(sourceSection, targetSection, itemIds, lessons);
      copyQuizzes(sourceSection, targetSection, itemIds);
      copyLiveClassSlots(sourceSection, targetSection, itemIds);
      copySectionItems(sourceSection, targetSection, itemIds);
    }

    if (source.getTemplate().getPreviewLesson() != null) {
      target.setPreviewLesson(lessons.get(source.getTemplate().getPreviewLesson().getId()));
      target = templateRepository.save(target);
    }

    return courseRepository.save(
        Course.builder().template(target).slug(slug).name(source.getTitle()).createdBy(createdBy).build());
  }

  private CourseTemplate copyTemplate(CourseTemplate source) {
    return CourseTemplate.builder()
        .title(source.getTitle())
        .titleEn(source.getTitleEn())
        .thumbnailObjectKey(source.getThumbnailObjectKey())
        .shortDescription(source.getShortDescription())
        .shortDescriptionEn(source.getShortDescriptionEn())
        .description(source.getDescription())
        .descriptionEn(source.getDescriptionEn())
        .highlights(copyList(source.getHighlights()))
        .highlightsEn(copyList(source.getHighlightsEn()))
        .courseOutcomes(copyList(source.getCourseOutcomes()))
        .courseOutcomesEn(copyList(source.getCourseOutcomesEn()))
        .requirements(copyList(source.getRequirements()))
        .requirementsEn(copyList(source.getRequirementsEn()))
        .prerequisites(copyList(source.getPrerequisites()))
        .prerequisitesEn(copyList(source.getPrerequisitesEn()))
        .level(source.getLevel())
        .language(source.getLanguage())
        .liveSessionCount(source.getLiveSessionCount())
        .quizCount(source.getQuizCount())
        .recordedHoursCount(source.getRecordedHoursCount())
        .estimatedDurationMinutes(source.getEstimatedDurationMinutes())
        .targetAudience(source.getTargetAudience())
        .targetAudienceEn(source.getTargetAudienceEn())
        .build();
  }

  private void copyCategories(Course source, CourseTemplate target) {
    categoryRepository.saveAll(
        categoryRepository.findByCourseId(source.getId()).stream()
            .map(
                sourceCategory ->
                    CourseCategory.builder()
                        .template(target)
                        .category(sourceCategory.getCategory())
                        .build())
            .toList());
  }

  private CourseSection copySection(CourseSection source, CourseTemplate target) {
    return CourseSection.builder()
        .template(target)
        .title(source.getTitle())
        .titleEn(source.getTitleEn())
        .slug(source.getSlug())
        .position(source.getPosition())
        .description(source.getDescription())
        .descriptionEn(source.getDescriptionEn())
        .status(source.getStatus())
        .publishedAt(source.getPublishedAt())
        .isMandatory(source.getIsMandatory())
        .isFree(source.getIsFree())
        .releaseType(copiedReleaseType(source.getReleaseType()))
        .releaseAt(copiedReleaseAt(source.getReleaseType(), source.getReleaseAt()))
        .unlockAfterDays(copiedUnlockAfterDays(source.getReleaseType(), source.getUnlockAfterDays()))
        .build();
  }

  private void copyLessons(
      CourseSection sourceSection,
      CourseSection targetSection,
      Map<UUID, UUID> itemIds,
      Map<UUID, Lesson> lessons) {
    for (Lesson source :
        lessonRepository.findBySectionIdOrderByPositionAsc(sourceSection.getId())) {
      Lesson target =
          lessonRepository.save(
              Lesson.builder()
                  .section(targetSection)
                  .title(source.getTitle())
                  .titleEn(source.getTitleEn())
                  .slug(source.getSlug())
                  .position(source.getPosition())
                  .lessonType(source.getLessonType())
                  .status(source.getStatus())
                  .durationSeconds(source.getDurationSeconds())
                  .transcriptUrl(source.getTranscriptUrl())
                  .isFree(source.getIsFree())
                  .isMandatory(source.getIsMandatory())
                  .releaseType(copiedReleaseType(source.getReleaseType()))
                  .releaseAt(copiedReleaseAt(source.getReleaseType(), source.getReleaseAt()))
                  .unlockAfterDays(
                      copiedUnlockAfterDays(source.getReleaseType(), source.getUnlockAfterDays()))
                  .build());
      lessons.put(source.getId(), target);
      itemIds.put(source.getId(), target.getId());
      copyLessonResources(source, target);
      copyMediaAsset(source, target);
    }
  }

  private void copyLessonResources(Lesson source, Lesson target) {
    resourceRepository.saveAll(
        resourceRepository.findByLessonIdOrderByPositionAsc(source.getId()).stream()
            .map(
                resource ->
                    LessonResource.builder()
                        .lesson(target)
                        .resourceType(resource.getResourceType())
                        .purpose(resource.getPurpose())
                        .title(resource.getTitle())
                        .titleEn(resource.getTitleEn())
                        .fileUrl(resource.getFileUrl())
                        .fileObjectKey(resource.getFileObjectKey())
                        .mimeType(resource.getMimeType())
                        .position(resource.getPosition())
                        .build())
            .toList());
  }

  private void copyMediaAsset(Lesson source, Lesson target) {
    mediaAssetRepository
        .findByLessonId(source.getId())
        .ifPresent(
            media -> {
              if (media.getStatus() != MediaStatus.READY) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Course cannot be copied while a lesson media asset is still processing");
              }
              mediaAssetRepository.save(
                  MediaAsset.builder()
                      .lesson(target)
                      .provider(media.getProvider())
                      .assetType(media.getAssetType())
                      .providerAssetId(media.getProviderAssetId())
                      .providerLibraryId(media.getProviderLibraryId())
                      .playbackId(media.getPlaybackId())
                      .playbackPolicy(media.getPlaybackPolicy())
                      .fileUrl(media.getFileUrl())
                      .title(media.getTitle())
                      .titleEn(media.getTitleEn())
                      .thumbnailObjectKey(media.getThumbnailObjectKey())
                      .maxResolution(media.getMaxResolution())
                      .durationSec(media.getDurationSec())
                      .status(media.getStatus())
                      .createdBy(media.getCreatedBy())
                      .preferredPlaybackMode(media.getPreferredPlaybackMode())
                      .build());
            });
  }

  private void copyQuizzes(
      CourseSection sourceSection, CourseSection targetSection, Map<UUID, UUID> itemIds) {
    for (Quiz source : quizRepository.findBySectionIdOrderByPositionAsc(sourceSection.getId())) {
      Quiz target =
          quizRepository.save(
              Quiz.builder()
                  .section(targetSection)
                  .position(source.getPosition())
                  .title(source.getTitle())
                  .titleEn(source.getTitleEn())
                  .passingScorePct(source.getPassingScorePct())
                  .maxAttempts(source.getMaxAttempts())
                  .timeLimitSec(source.getTimeLimitSec())
                  .status(source.getStatus())
                  .build());
      itemIds.put(source.getId(), target.getId());
      copyQuestions(source, target);
    }
  }

  private void copyQuestions(Quiz source, Quiz target) {
    for (QuizQuestion question :
        questionRepository.findByQuizIdOrderByPositionAsc(source.getId())) {
      QuizQuestion targetQuestion =
          questionRepository.save(
              QuizQuestion.builder()
                  .quiz(target)
                  .position(question.getPosition())
                  .questionText(question.getQuestionText())
                  .questionTextEn(question.getQuestionTextEn())
                  .questionType(question.getQuestionType())
                  .points(question.getPoints())
                  .explanationText(question.getExplanationText())
                  .explanationTextEn(question.getExplanationTextEn())
                  .build());
      choiceRepository.saveAll(
          choiceRepository.findByQuestionId(question.getId()).stream()
              .map(choice -> copyChoice(choice, targetQuestion))
              .toList());
    }
  }

  private QuizChoice copyChoice(QuizChoice source, QuizQuestion targetQuestion) {
    return QuizChoice.builder()
        .question(targetQuestion)
        .choiceText(source.getChoiceText())
        .choiceTextEn(source.getChoiceTextEn())
        .isCorrect(source.getIsCorrect())
        .build();
  }

  private void copyLiveClassSlots(
      CourseSection sourceSection, CourseSection targetSection, Map<UUID, UUID> itemIds) {
    for (LiveClassSlot source : liveClassSlotRepository.findBySectionId(sourceSection.getId())) {
      LiveClassSlot target =
          liveClassSlotRepository.save(
              LiveClassSlot.builder()
                  .section(targetSection)
                  .title(source.getTitle())
                  .titleEn(source.getTitleEn())
                  .description(source.getDescription())
                  .descriptionEn(source.getDescriptionEn())
                  .expectedDurationMinutes(source.getExpectedDurationMinutes())
                  .isMandatory(source.getIsMandatory())
                  .build());
      itemIds.put(source.getId(), target.getId());
    }
  }

  private void copySectionItems(
      CourseSection sourceSection, CourseSection targetSection, Map<UUID, UUID> itemIds) {
    sectionItemRepository.saveAll(
        sectionItemRepository.findBySectionIdOrderByPositionAsc(sourceSection.getId()).stream()
            .map(item -> copySectionItem(item, targetSection, itemIds))
            .toList());
  }

  private SectionItem copySectionItem(
      SectionItem source, CourseSection targetSection, Map<UUID, UUID> itemIds) {
    UUID targetItemId = itemIds.get(source.getItemId());
    if (targetItemId == null) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Curriculum contains an invalid " + source.getItemType() + " section item");
    }
    return SectionItem.builder()
        .section(targetSection)
        .itemType(source.getItemType())
        .itemId(targetItemId)
        .position(source.getPosition())
        .build();
  }

  private List<String> copyList(List<String> values) {
    return values == null ? null : new ArrayList<>(values);
  }

  private ReleaseType copiedReleaseType(ReleaseType releaseType) {
    return releaseType == ReleaseType.FIXED_DATE ? ReleaseType.IMMEDIATE : releaseType;
  }

  private java.time.Instant copiedReleaseAt(ReleaseType releaseType, java.time.Instant releaseAt) {
    return releaseType == ReleaseType.FIXED_DATE ? null : releaseAt;
  }

  private Integer copiedUnlockAfterDays(ReleaseType releaseType, Integer unlockAfterDays) {
    return releaseType == ReleaseType.FIXED_DATE ? null : unlockAfterDays;
  }
}
