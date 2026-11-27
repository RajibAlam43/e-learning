package com.gii.common.service.payment;

import com.gii.common.entity.course.Course;
import com.gii.common.entity.order.Order;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CoursePrerequisiteEligibilityService {
  private final JdbcTemplate jdbc;

  public record Requirement(UUID courseTemplateId, String title, int credits) {}

  public List<Requirement> missing(UUID userId, UUID templateId) {
    return jdbc.query(
        """
        SELECT t.id, COALESCE(NULLIF(BTRIM(t.title_en),''),t.title) title,t.credits
        FROM course_prerequisite_links p JOIN course_templates t ON t.id=p.prerequisite_template_id
        WHERE p.course_template_id=? AND NOT EXISTS (
         SELECT 1 FROM academic_achievements a WHERE a.user_id=? AND a.course_template_id=t.id
        ) AND NOT EXISTS (
         SELECT 1 FROM enrollments e JOIN course_offerings c ON c.id=e.course_offering_id
         WHERE e.user_id=? AND e.completed_at IS NOT NULL AND c.course_template_id=t.id
        ) ORDER BY title,t.id
        """,
        (r, n) ->
            new Requirement(
                r.getObject("id", UUID.class), r.getString("title"), r.getInt("credits")),
        templateId,
        userId,
        userId);
  }

  public boolean hasCredit(UUID userId, UUID templateId) {
    return Boolean.TRUE.equals(
        jdbc.queryForObject(
            """
            SELECT EXISTS(SELECT 1 FROM academic_achievements
              WHERE user_id=? AND course_template_id=?)
            """,
            Boolean.class,
            userId,
            templateId));
  }

  public void validate(UUID userId, Course course, UUID orderId) {
    if (orderId != null
        && Boolean.TRUE.equals(
            jdbc.queryForObject(
                """
                SELECT EXISTS(SELECT 1 FROM order_course_eligibility e JOIN orders o ON o.id=e.order_id
                 WHERE e.order_id=? AND e.course_id=? AND e.user_id=? AND e.course_template_id=? AND o.user_id=e.user_id)
                """,
                Boolean.class,
                orderId,
                course.getId(),
                userId,
                course.getTemplate().getId()))) {
      return;
    }
    jdbc.execute("SELECT pg_advisory_xact_lock_shared(71925410)");
    var missing = missing(userId, course.getTemplate().getId());
    if (!missing.isEmpty()) {
      throw new EnrollmentPolicyViolationException(
          "Complete the required courses or obtain a credit waiver: "
              + String.join(", ", missing.stream().map(Requirement::title).toList()));
    }
  }

  @Transactional
  public void recordForOrder(Order order, List<Course> courses) {
    for (var course : courses) {
      boolean active =
          Boolean.TRUE.equals(
              jdbc.queryForObject(
                  """
                  SELECT EXISTS(SELECT 1 FROM enrollments
                    WHERE user_id=? AND course_offering_id=? AND status='ACTIVE'
                      AND (expires_at IS NULL OR expires_at>now()))
                  """,
                  Boolean.class,
                  order.getUser().getId(),
                  course.getId()));
      if (!active) {
        validate(order.getUser().getId(), course, null);
      }
      jdbc.execute("SELECT pg_advisory_xact_lock_shared(71925410)");
      jdbc.update(
          """
          INSERT INTO order_course_eligibility(order_id,course_id,user_id,course_template_id,prerequisite_template_ids)
          SELECT ?,?,?,?,COALESCE(jsonb_agg(prerequisite_template_id),'[]'::jsonb)
          FROM course_prerequisite_links WHERE course_template_id=?
          ON CONFLICT DO NOTHING
          """,
          order.getId(),
          course.getId(),
          order.getUser().getId(),
          course.getTemplate().getId(),
          course.getTemplate().getId());
    }
  }
}
