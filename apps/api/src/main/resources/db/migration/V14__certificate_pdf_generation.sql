ALTER TABLE certificates
    ADD COLUMN pdf_object_key text,
    ADD COLUMN collection_enrollment_id uuid,
    ADD COLUMN instructor_name text;

ALTER TABLE certificates
    ADD CONSTRAINT fk_certificates_collection_enrollment
        FOREIGN KEY (collection_enrollment_id) REFERENCES collection_enrollments (id) ON DELETE RESTRICT;

UPDATE certificates c
SET collection_enrollment_id = ce.id
FROM collection_enrollments ce
WHERE c.target_type = 'COLLECTION'
  AND ce.collection_id = c.collection_id
  AND ce.user_id = c.user_id;

CREATE UNIQUE INDEX uk_certificates_enrollment_not_null
    ON certificates (enrollment_id)
    WHERE enrollment_id IS NOT NULL;

CREATE UNIQUE INDEX uk_certificates_collection_enrollment_not_null
    ON certificates (collection_enrollment_id)
    WHERE collection_enrollment_id IS NOT NULL;

CREATE UNIQUE INDEX uk_certificate_templates_active_name
    ON certificate_templates (name)
    WHERE is_active = TRUE;

INSERT INTO certificate_templates (name, template_json, is_active)
SELECT 'Default Course Certificate v1',
       '{"targetType":"COURSE","version":"v1","resourcePath":"default-course-certificate-v1.html"}'::jsonb,
       TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM certificate_templates WHERE name = 'Default Course Certificate v1'
);

INSERT INTO certificate_templates (name, template_json, is_active)
SELECT 'Default Program Certificate v1',
       '{"targetType":"COLLECTION","version":"v1","resourcePath":"default-program-certificate-v1.html"}'::jsonb,
       TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM certificate_templates WHERE name = 'Default Program Certificate v1'
);
