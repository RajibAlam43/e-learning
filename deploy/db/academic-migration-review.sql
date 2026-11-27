-- Read-only review AFTER V24-V29 on a restored copy, before production promotion.
-- This file never awards credits, changes access, or modifies a purchased contract.
BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ READ ONLY;
SET LOCAL statement_timeout = '60s';

WITH historical AS (
    SELECT e.user_id, c.course_template_id, e.id AS enrollment_id, c.id AS course_id,
           e.completed_at, e.status, c.semester_id, t.credits AS current_credits,
           COALESCE(NULLIF(btrim(t.title_en), ''), t.title) AS title,
           row_number() OVER (
               PARTITION BY e.user_id, c.course_template_id ORDER BY e.completed_at, e.id) AS priority,
           count(*) OVER (PARTITION BY e.user_id, c.course_template_id) AS completed_cohorts
    FROM enrollments e
    JOIN course_offerings c ON c.id = e.course_offering_id
    JOIN course_templates t ON t.id = c.course_template_id
    WHERE e.completed_at IS NOT NULL AND NOT EXISTS (
        SELECT 1 FROM academic_achievements a
        WHERE a.user_id = e.user_id AND a.course_template_id = c.course_template_id)
), candidates AS (
    SELECT user_id, course_template_id, enrollment_id, course_id, title, completed_at,
           status, semester_id, current_credits, completed_cohorts,
           'REVIEW_REQUIRED: current credits are not proof of historical credit value' AS decision
    FROM historical WHERE priority = 1
), missing_purchase_courses AS (
    SELECT s.order_item_id, s.course_offering_id
    FROM order_item_courses s JOIN order_items i ON i.id = s.order_item_id
    WHERE i.item_type = 'COLLECTION' AND NOT EXISTS (
        SELECT 1 FROM jsonb_array_elements(COALESCE(i.program_snapshot->'courses', '[]'::jsonb)) entry
        WHERE entry->>'courseId' = s.course_offering_id::text)
)
SELECT jsonb_pretty(jsonb_build_object(
    'generated_at', now(),
    'transaction_read_only', current_setting('transaction_read_only'),
    'latest_migration', (SELECT version FROM flyway_schema_history
        WHERE success AND version IS NOT NULL ORDER BY installed_rank DESC LIMIT 1),
    'inventory', jsonb_build_object(
        'course_enrollments', (SELECT count(*) FROM enrollments),
        'completed_course_enrollments', (SELECT count(*) FROM enrollments WHERE completed_at IS NOT NULL),
        'program_enrollments', (SELECT count(*) FROM collection_enrollments),
        'orders', (SELECT count(*) FROM orders),
        'paid_orders', (SELECT count(*) FROM orders WHERE status = 'PAID'),
        'order_items', (SELECT count(*) FROM order_items),
        'certificates', (SELECT count(*) FROM certificates),
        'revoked_certificates', (SELECT count(*) FROM certificates WHERE revoked_at IS NOT NULL),
        'academic_awards', (SELECT count(*) FROM academic_achievements)),
    'review_counts', jsonb_build_object(
        'historical_credit_candidates', (SELECT count(*) FROM candidates),
        'cohorts_without_semester', (SELECT count(*) FROM course_offerings
            WHERE study_mode = 'COHORT_BASED' AND semester_id IS NULL),
        'program_enrollments_without_snapshot', (SELECT count(*) FROM collection_enrollments
            WHERE program_snapshot IS NULL),
        'collection_order_items_without_snapshot', (SELECT count(*) FROM order_items
            WHERE item_type = 'COLLECTION' AND program_snapshot IS NULL),
        'purchased_courses_missing_from_snapshot', (SELECT count(*) FROM missing_purchase_courses)),
    'historical_credit_candidates', COALESCE((SELECT jsonb_agg(to_jsonb(c)
        ORDER BY c.user_id, c.course_template_id) FROM candidates c), '[]'::jsonb),
    'purchased_courses_missing_from_snapshot', COALESCE((SELECT jsonb_agg(to_jsonb(m))
        FROM missing_purchase_courses m), '[]'::jsonb),
    'historical_backfill_applied', false
)) AS review;

ROLLBACK;
