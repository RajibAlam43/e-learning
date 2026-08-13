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
