CREATE TABLE student_learning_streaks (
    user_id uuid PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    current_streak integer NOT NULL,
    max_streak integer NOT NULL,
    last_activity_date date NOT NULL,
    last_activity_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT chk_student_learning_streaks_current_positive CHECK (current_streak > 0),
    CONSTRAINT chk_student_learning_streaks_max_valid CHECK (max_streak >= current_streak)
);

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
