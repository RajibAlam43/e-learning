ALTER TABLE live_classes ADD COLUMN lead_instructor_id uuid REFERENCES users(id);

UPDATE live_classes lc
SET lead_instructor_id = (
    SELECT ci.instructor_user_id
    FROM course_offering_instructors ci
    WHERE ci.course_offering_id = lc.course_offering_id
    ORDER BY CASE WHEN ci.role = 'PRIMARY' THEN 0 ELSE 1 END, ci.instructor_user_id
    LIMIT 1
);

CREATE INDEX idx_live_class_lead_schedule ON live_classes(lead_instructor_id, starts_at, ends_at)
    WHERE status IN ('SCHEDULED', 'LIVE');
