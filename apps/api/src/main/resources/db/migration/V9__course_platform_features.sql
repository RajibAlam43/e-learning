UPDATE categories
SET name_en = name
WHERE name_en IS NULL OR btrim(name_en) = '';

ALTER TABLE categories
    ALTER COLUMN name_en SET NOT NULL,
    ADD CONSTRAINT chk_categories_name_not_blank CHECK (btrim(name) <> ''),
    ADD CONSTRAINT chk_categories_name_en_not_blank CHECK (btrim(name_en) <> '');

CREATE TABLE course_reviews (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id uuid NOT NULL,
    user_id uuid NOT NULL,
    rating integer NOT NULL,
    review_text varchar(5000) NOT NULL,
    status varchar(30) NOT NULL DEFAULT 'PENDING',
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_course_reviews_course
        FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE CASCADE,
    CONSTRAINT fk_course_reviews_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uk_course_reviews_course_user UNIQUE (course_id, user_id),
    CONSTRAINT chk_course_reviews_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT chk_course_reviews_text_not_blank CHECK (btrim(review_text) <> ''),
    CONSTRAINT chk_course_reviews_status
        CHECK (status IN ('PENDING', 'PUBLISHED', 'UNPUBLISHED'))
);

CREATE INDEX idx_course_reviews_course_status_created_at
    ON course_reviews (course_id, status, created_at DESC);
CREATE INDEX idx_course_reviews_status_created_at
    ON course_reviews (status, created_at DESC);

CREATE INDEX idx_support_tickets_email_created_at
    ON support_tickets (email, created_at DESC)
    WHERE email IS NOT NULL;
CREATE INDEX idx_support_tickets_phone_created_at
    ON support_tickets (phone, created_at DESC)
    WHERE phone IS NOT NULL;

ALTER TABLE lesson_resources ADD COLUMN file_object_key text;

UPDATE lesson_resources
SET file_object_key = file_url
WHERE file_object_key IS NULL;

ALTER TABLE lesson_resources ALTER COLUMN file_object_key SET NOT NULL;

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

ALTER TABLE support_tickets
    ADD COLUMN rate_limit_key_hash varchar(64),
    ADD CONSTRAINT chk_support_ticket_rate_limit_hash
        CHECK (rate_limit_key_hash IS NULL OR rate_limit_key_hash ~ '^[0-9a-f]{64}$');

CREATE INDEX idx_support_tickets_rate_limit_key_created_at
    ON support_tickets (rate_limit_key_hash, created_at DESC)
    WHERE rate_limit_key_hash IS NOT NULL;
