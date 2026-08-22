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
