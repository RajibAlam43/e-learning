CREATE TABLE manual_enrollment_audit (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL,
    request_id uuid NOT NULL UNIQUE,
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    actor_id uuid NOT NULL REFERENCES users(id),
    course_id uuid REFERENCES course_offerings(id),
    program_id uuid REFERENCES collections(id),
    program_funded boolean NOT NULL,
    override_dates boolean NOT NULL,
    override_capacity boolean NOT NULL,
    allow_unpublished boolean NOT NULL,
    reason text NOT NULL CHECK (length(trim(reason)) BETWEEN 1 AND 5000),
    enrollment_id uuid NOT NULL,
    registration_id uuid REFERENCES program_course_registrations(id),
    access_expires_at timestamptz,
    changed boolean NOT NULL,
    CHECK ((program_funded AND course_id IS NOT NULL AND program_id IS NOT NULL)
        OR (NOT program_funded AND ((course_id IS NULL) <> (program_id IS NULL))))
);
CREATE INDEX ix_manual_enrollment_audit_user_time
    ON manual_enrollment_audit(user_id, created_at DESC);
