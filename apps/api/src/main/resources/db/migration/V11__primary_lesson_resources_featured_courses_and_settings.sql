ALTER TABLE lesson_resources
    ADD COLUMN purpose varchar(30) NOT NULL DEFAULT 'SUPPLEMENTARY';

WITH first_pdf_resource AS (
    SELECT DISTINCT ON (lr.lesson_id) lr.id
    FROM lesson_resources lr
    JOIN lessons l ON l.id = lr.lesson_id
    WHERE l.lesson_type = 'PDF' AND lr.resource_type = 'PDF'
    ORDER BY lr.lesson_id, lr.position, lr.created_at, lr.id
)
UPDATE lesson_resources
SET purpose = 'PRIMARY_CONTENT'
WHERE id IN (SELECT id FROM first_pdf_resource);

ALTER TABLE lesson_resources
    ADD CONSTRAINT chk_lesson_resources_purpose
        CHECK (purpose IN ('PRIMARY_CONTENT', 'SUPPLEMENTARY')),
    ADD CONSTRAINT chk_lesson_resources_primary_is_pdf
        CHECK (purpose <> 'PRIMARY_CONTENT' OR resource_type = 'PDF');

CREATE UNIQUE INDEX uk_lesson_resources_primary_content
    ON lesson_resources (lesson_id)
    WHERE purpose = 'PRIMARY_CONTENT';

ALTER TABLE courses
    ADD COLUMN is_featured boolean NOT NULL DEFAULT false,
    ADD COLUMN featured_position integer,
    ADD COLUMN featured_at timestamptz,
    ADD CONSTRAINT chk_courses_featured_position
        CHECK (featured_position IS NULL OR featured_position > 0),
    ADD CONSTRAINT chk_courses_featured_fields
        CHECK (
            (is_featured AND featured_position IS NOT NULL AND featured_at IS NOT NULL)
            OR
            (NOT is_featured AND featured_position IS NULL AND featured_at IS NULL)
        );

CREATE INDEX idx_courses_public_featured
    ON courses (featured_position, featured_at DESC, id DESC)
    WHERE status = 'PUBLISHED' AND is_featured;

CREATE TABLE app_settings (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    setting_key varchar(150) NOT NULL UNIQUE,
    value_json jsonb NOT NULL,
    description varchar(1000),
    is_public boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT chk_app_settings_key
        CHECK (setting_key ~ '^[a-z0-9][a-z0-9._-]{0,149}$'),
    CONSTRAINT chk_app_settings_value_object
        CHECK (jsonb_typeof(value_json) = 'object')
);

CREATE INDEX idx_app_settings_public_key
    ON app_settings (setting_key)
    WHERE is_public;
