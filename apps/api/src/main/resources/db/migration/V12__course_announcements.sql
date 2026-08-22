CREATE TABLE course_announcements (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id uuid NOT NULL REFERENCES courses (id) ON DELETE CASCADE,
    created_by uuid NOT NULL REFERENCES users (id),
    title varchar(200) NOT NULL,
    content text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_course_announcements_course_created
    ON course_announcements (course_id, created_at DESC);

CREATE INDEX idx_course_announcements_created
    ON course_announcements (created_at DESC);
