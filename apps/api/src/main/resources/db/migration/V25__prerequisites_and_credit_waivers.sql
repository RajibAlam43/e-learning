CREATE TABLE course_prerequisite_links (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    course_template_id uuid NOT NULL REFERENCES course_templates(id) ON DELETE CASCADE,
    prerequisite_template_id uuid NOT NULL REFERENCES course_templates(id),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (course_template_id, prerequisite_template_id),
    CHECK (course_template_id <> prerequisite_template_id)
);
CREATE INDEX idx_prerequisite_required ON course_prerequisite_links(prerequisite_template_id);

CREATE TABLE course_waiver_requests (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL REFERENCES users(id),
    course_template_id uuid NOT NULL REFERENCES course_templates(id),
    course_title text NOT NULL,
    requested_credits integer NOT NULL CHECK (requested_credits > 0),
    reason text NOT NULL,
    evidence_object_key text,
    status varchar(20) NOT NULL CHECK (status IN ('PENDING','APPROVED','REJECTED')),
    review_reason text,
    reviewer_id uuid REFERENCES users(id),
    reviewed_at timestamptz,
    achievement_id uuid REFERENCES academic_achievements(id) ON DELETE SET NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CHECK ((status = 'PENDING' AND reviewed_at IS NULL AND reviewer_id IS NULL)
        OR (status <> 'PENDING' AND reviewed_at IS NOT NULL AND reviewer_id IS NOT NULL
            AND review_reason IS NOT NULL))
);
CREATE UNIQUE INDEX uk_pending_waiver ON course_waiver_requests(user_id, course_template_id)
    WHERE status = 'PENDING';
CREATE INDEX idx_waivers_user ON course_waiver_requests(user_id, created_at);
CREATE INDEX idx_waivers_status ON course_waiver_requests(status, created_at);

ALTER TABLE academic_achievements DROP CONSTRAINT academic_achievements_source_check;
ALTER TABLE academic_achievements ADD CONSTRAINT academic_achievements_source_check
    CHECK (source IN ('COMPLETION','EXEMPTION'));
ALTER TABLE academic_achievements ADD COLUMN waiver_request_id uuid REFERENCES course_waiver_requests(id) ON DELETE SET NULL;
ALTER TABLE academic_achievements ADD COLUMN awarded_by uuid REFERENCES users(id);
ALTER TABLE academic_achievements ADD COLUMN award_reason text;

-- Immutable prerequisite eligibility captured when the order was created. Existing
-- orders predate structured prerequisites and retain their original eligibility.
CREATE TABLE order_course_eligibility (
    order_id uuid NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    course_id uuid NOT NULL REFERENCES course_offerings(id),
    user_id uuid NOT NULL REFERENCES users(id),
    course_template_id uuid NOT NULL REFERENCES course_templates(id),
    prerequisite_template_ids jsonb NOT NULL DEFAULT '[]'::jsonb,
    checked_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (order_id, course_id)
);
INSERT INTO order_course_eligibility (order_id, course_id, user_id, course_template_id, checked_at)
SELECT DISTINCT o.id, c.id, o.user_id, c.course_template_id, o.created_at
FROM orders o JOIN order_items i ON i.order_id=o.id
JOIN course_offerings c ON c.id=i.course_offering_id
WHERE o.status IN ('PENDING','PAID')
ON CONFLICT DO NOTHING;
INSERT INTO order_course_eligibility (order_id, course_id, user_id, course_template_id, checked_at)
SELECT DISTINCT o.id, c.id, o.user_id, c.course_template_id, o.created_at
FROM orders o JOIN order_items i ON i.order_id=o.id
JOIN order_item_courses ic ON ic.order_item_id=i.id
JOIN course_offerings c ON c.id=ic.course_offering_id
WHERE o.status IN ('PENDING','PAID')
ON CONFLICT DO NOTHING;
