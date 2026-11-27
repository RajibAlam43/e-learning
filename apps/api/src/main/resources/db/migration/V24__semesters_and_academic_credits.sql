CREATE TABLE academic_semesters (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    code varchar(40) NOT NULL UNIQUE,
    name varchar(150) NOT NULL,
    starts_at timestamptz NOT NULL,
    ends_at timestamptz NOT NULL,
    registration_starts_at timestamptz NOT NULL,
    registration_ends_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT chk_semester_dates CHECK (ends_at > starts_at),
    CONSTRAINT chk_semester_registration_dates CHECK (
        registration_ends_at > registration_starts_at AND registration_ends_at <= ends_at)
);

ALTER TABLE course_templates ADD COLUMN credits integer NOT NULL DEFAULT 3;
ALTER TABLE course_templates ADD CONSTRAINT chk_course_credits_positive CHECK (credits > 0);

-- Existing cohorts remain unassigned until an administrator selects their semester.
ALTER TABLE course_offerings ADD COLUMN semester_id uuid
    REFERENCES academic_semesters(id);
CREATE INDEX idx_course_offerings_semester ON course_offerings(semester_id);

CREATE TABLE academic_achievements (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    course_template_id uuid NOT NULL REFERENCES course_templates(id) ON DELETE CASCADE,
    source_enrollment_id uuid REFERENCES enrollments(id) ON DELETE SET NULL,
    source_course_id uuid REFERENCES course_offerings(id) ON DELETE SET NULL,
    semester_id uuid REFERENCES academic_semesters(id),
    course_title text NOT NULL,
    credits integer NOT NULL CHECK (credits > 0),
    source varchar(30) NOT NULL CHECK (source IN ('COMPLETION')),
    achieved_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uk_academic_achievement_user_template UNIQUE (user_id, course_template_id)
);
-- Historical completions deliberately receive no automatic credit backfill.
