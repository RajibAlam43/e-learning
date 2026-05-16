-- =========================================
-- V7: Certificates can target COURSE or COLLECTION
-- =========================================

ALTER TABLE certificates
    ADD COLUMN target_type varchar(20) NOT NULL DEFAULT 'COURSE',
    ADD COLUMN collection_id uuid,
    ADD COLUMN target_slug text;

ALTER TABLE certificates
    DROP CONSTRAINT IF EXISTS fk_certificates_course;

ALTER TABLE certificates
    ALTER COLUMN course_id DROP NOT NULL;

ALTER TABLE certificates
    ADD CONSTRAINT fk_certificates_course FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_certificates_collection FOREIGN KEY (collection_id) REFERENCES collections (id) ON DELETE RESTRICT;

ALTER TABLE certificates
    DROP CONSTRAINT IF EXISTS uk_certificates_user_course;

DROP INDEX IF EXISTS idx_certificates_course_id;

ALTER TABLE certificates
    RENAME COLUMN course_title TO target_title;

UPDATE certificates c
SET target_slug = cr.slug
FROM courses cr
WHERE c.course_id = cr.id;

ALTER TABLE certificates
    ALTER COLUMN target_slug SET NOT NULL;

ALTER TABLE certificates
    ADD CONSTRAINT chk_certificates_target_type CHECK (target_type IN ('COURSE', 'COLLECTION')),
    ADD CONSTRAINT chk_certificates_target_refs CHECK (
        (target_type = 'COURSE' AND course_id IS NOT NULL AND collection_id IS NULL)
        OR (target_type = 'COLLECTION' AND course_id IS NULL AND collection_id IS NOT NULL)
    );

CREATE UNIQUE INDEX uk_certificates_user_course_target
    ON certificates (user_id, course_id)
    WHERE target_type = 'COURSE' AND course_id IS NOT NULL;

CREATE UNIQUE INDEX uk_certificates_user_collection_target
    ON certificates (user_id, collection_id)
    WHERE target_type = 'COLLECTION' AND collection_id IS NOT NULL;

CREATE INDEX idx_certificates_course_id ON certificates (course_id);
CREATE INDEX idx_certificates_collection_id ON certificates (collection_id);
CREATE INDEX idx_certificates_target_type ON certificates (target_type);
