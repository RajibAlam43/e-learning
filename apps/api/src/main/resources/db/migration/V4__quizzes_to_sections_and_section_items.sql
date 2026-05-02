-- =========================================
-- V4: Quizzes belong to sections + mixed section item ordering
-- =========================================

-- 1) Quizzes move from lesson-level to section-level
ALTER TABLE quizzes
    DROP CONSTRAINT IF EXISTS fk_quizzes_lesson_course;

DROP INDEX IF EXISTS uk_quizzes_lesson_id_not_null;
DROP INDEX IF EXISTS uk_quizzes_course_id_when_lesson_null;
DROP INDEX IF EXISTS idx_quizzes_lesson_id;

ALTER TABLE quizzes
    DROP COLUMN IF EXISTS lesson_id;

ALTER TABLE quizzes
    ADD COLUMN section_id uuid NOT NULL,
    ADD COLUMN position integer NOT NULL;

ALTER TABLE quizzes
    ADD CONSTRAINT fk_quizzes_section_course
    FOREIGN KEY (section_id, course_id) REFERENCES course_sections (id, course_id) ON DELETE CASCADE;

ALTER TABLE quizzes
    ADD CONSTRAINT chk_quizzes_position_positive CHECK (position > 0);

CREATE INDEX idx_quizzes_section_id ON quizzes (section_id);
CREATE UNIQUE INDEX uk_quizzes_section_position ON quizzes (section_id, position);

-- 2) Canonical mixed ordering for lessons + quizzes inside section
CREATE TABLE section_items (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid (),
    section_id uuid NOT NULL,
    item_type varchar(20) NOT NULL,
    item_id uuid NOT NULL,
    position integer NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_section_items_section FOREIGN KEY (section_id) REFERENCES course_sections (id) ON DELETE CASCADE,
    CONSTRAINT chk_section_items_item_type CHECK (item_type IN ('LESSON', 'QUIZ')),
    CONSTRAINT chk_section_items_position_positive CHECK (position > 0),
    CONSTRAINT uk_section_items_section_position UNIQUE (section_id, position),
    CONSTRAINT uk_section_items_type_item UNIQUE (item_type, item_id)
);

CREATE INDEX idx_section_items_section_id ON section_items (section_id);

