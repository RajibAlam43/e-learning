package com.gii.api.service.academic;

import com.gii.api.model.response.academic.PrerequisiteResponse;
import com.gii.api.model.response.academic.PrerequisiteResponse.CourseOption;
import com.gii.common.entity.academic.CoursePrerequisite;
import com.gii.common.repository.academic.CoursePrerequisiteRepository;
import com.gii.common.repository.course.CourseRepository;
import com.gii.common.repository.course.CourseTemplateRepository;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CoursePrerequisiteManagementService {
  private final CourseRepository courses;
  private final CourseTemplateRepository templates;
  private final CoursePrerequisiteRepository links;
  private final JdbcTemplate jdbc;

  public PrerequisiteResponse get(UUID courseId) {
    return getTemplate(templateId(courseId));
  }

  private UUID templateId(UUID courseId) {
    return courses
        .findById(courseId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"))
        .getTemplate()
        .getId();
  }

  private PrerequisiteResponse getTemplate(UUID templateId) {
    var ids =
        links.findByCourseTemplateId(templateId).stream()
            .map(CoursePrerequisite::getPrerequisiteTemplateId)
            .toList();
    var options =
        templates.findAllById(ids).stream()
            .map(t ->
                new CourseOption(
                    t.getId(),
                    t.getTitleEn() != null && !t.getTitleEn().isBlank()
                        ? t.getTitleEn()
                        : t.getTitle(),
                    t.getCredits()))
            .sorted(Comparator.comparing(CourseOption::title))
            .toList();
    return new PrerequisiteResponse(templateId, options);
  }

  @Transactional
  public PrerequisiteResponse save(UUID courseId, List<UUID> required) {
    jdbc.execute("SELECT pg_advisory_xact_lock(71925410)");
    UUID id = templateId(courseId);
    var unique = new HashSet<>(required);
    if (unique.size() != required.size() || unique.contains(id)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Prerequisites must be distinct and cannot include the course itself");
    }
    if (templates.findAllById(unique).size() != unique.size()) {
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND, "A prerequisite course was not found");
    }
    Map<UUID, Set<UUID>> graph = new HashMap<>();
    links
        .findAll()
        .forEach(l ->
            graph
                .computeIfAbsent(l.getCourseTemplateId(), k -> new HashSet<>())
                .add(l.getPrerequisiteTemplateId()));
    graph.put(id, unique);
    for (UUID candidate : unique) {
      Set<UUID> visited = new HashSet<>();
      Deque<UUID> queue = new ArrayDeque<>();
      queue.add(candidate);
      while (!queue.isEmpty()) {
        UUID next = queue.remove();
        if (next.equals(id)) {
          throw new ResponseStatusException(
              HttpStatus.CONFLICT, "Prerequisites cannot form a cycle");
        }
        if (visited.add(next)) {
          queue.addAll(graph.getOrDefault(next, Set.of()));
        }
      }
    }
    links.deleteByCourseTemplateId(id);
    links.flush();
    links.saveAllAndFlush(
        unique.stream()
            .map(p ->
                CoursePrerequisite.builder().courseTemplateId(id).prerequisiteTemplateId(p).build())
            .toList());
    return getTemplate(id);
  }

  public List<CourseOption> waiverCourses() {
    return jdbc.query(
        """
        SELECT DISTINCT t.id,COALESCE(NULLIF(BTRIM(t.title_en),''),t.title) title,t.credits
        FROM course_templates t WHERE EXISTS(SELECT 1 FROM course_offerings c WHERE c.course_template_id=t.id AND c.status='PUBLISHED')
        OR EXISTS(SELECT 1 FROM course_prerequisite_links p JOIN course_offerings c ON c.course_template_id=p.course_template_id WHERE p.prerequisite_template_id=t.id AND c.status='PUBLISHED')
        ORDER BY title,t.id
        """,
        (r, n) ->
            new CourseOption(
                r.getObject("id", UUID.class), r.getString("title"), r.getInt("credits")));
  }
}
