ALTER TABLE enrollments
    ADD COLUMN program_access_managed boolean NOT NULL DEFAULT false,
    ADD COLUMN independent_access_available boolean NOT NULL DEFAULT false,
    ADD COLUMN independent_access_expires_at timestamptz;

CREATE TABLE program_course_registrations (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    collection_enrollment_id uuid NOT NULL REFERENCES collection_enrollments(id) ON DELETE CASCADE,
    course_template_id uuid NOT NULL REFERENCES course_templates(id),
    course_offering_id uuid NOT NULL REFERENCES course_offerings(id),
    enrollment_id uuid NOT NULL REFERENCES enrollments(id) ON DELETE CASCADE,
    credits integer NOT NULL CHECK (credits > 0),
    mandatory boolean NOT NULL,
    status varchar(20) NOT NULL CHECK (status IN ('ACTIVE','WITHDRAWN')),
    access_expires_at timestamptz,
    withdrawn_at timestamptz,
    withdrawn_by uuid REFERENCES users(id),
    withdrawal_reason text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CHECK ((status='ACTIVE' AND withdrawn_at IS NULL AND withdrawn_by IS NULL
            AND withdrawal_reason IS NULL)
        OR (status='WITHDRAWN' AND withdrawn_at IS NOT NULL AND withdrawn_by IS NOT NULL
            AND withdrawal_reason IS NOT NULL))
);
CREATE UNIQUE INDEX uk_active_program_template_registration
    ON program_course_registrations(collection_enrollment_id,course_template_id)
    WHERE status='ACTIVE';
CREATE INDEX idx_program_registration_enrollment ON program_course_registrations(enrollment_id);
CREATE INDEX idx_program_registration_history
    ON program_course_registrations(collection_enrollment_id,created_at);
