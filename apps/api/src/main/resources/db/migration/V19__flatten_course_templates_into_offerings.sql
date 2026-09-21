-- Remove course versioning entirely. course_template_versions already holds all the reusable
-- curriculum content (title, description, sections' parent, etc.) added in V12 — it just needs
-- its versioning-specific columns stripped and to become the one and only course_templates table.
-- The old identity-only course_templates (internal_key) is dropped outright: the template's UUID
-- is already a stable, unique identity, and nothing reads internal_key at runtime.
-- course_offerings, course_sections, and course_categories keep pointing at "the template", now
-- directly and without an intervening version. Courses become directly, freely mutable; the only
-- protection for existing learners is that enrollments.completed_at, once set, is permanent
-- (enforced in application code).

-- 1. Strip the versioning-specific columns; everything else on this table is real curriculum
--    content and stays untouched.
ALTER TABLE course_template_versions
    DROP CONSTRAINT course_template_versions_course_template_id_fkey,
    DROP CONSTRAINT uk_course_template_versions_template_version,
    DROP CONSTRAINT chk_course_template_versions_version_positive,
    DROP CONSTRAINT chk_course_template_versions_status,
    DROP COLUMN course_template_id,
    DROP COLUMN version_number,
    DROP COLUMN status,
    DROP COLUMN published_at;

ALTER TABLE course_template_versions
    RENAME CONSTRAINT chk_course_template_versions_level TO chk_course_templates_level;
ALTER TABLE course_template_versions
    RENAME CONSTRAINT chk_course_template_versions_language TO chk_course_templates_language;
ALTER TABLE course_template_versions
    RENAME CONSTRAINT chk_course_template_versions_counts_non_negative
        TO chk_course_templates_counts_non_negative;
ALTER TABLE course_template_versions
    RENAME CONSTRAINT chk_course_template_versions_duration_non_negative
        TO chk_course_templates_duration_non_negative;

-- 2. The old identity-only course_templates table (internal_key) is superseded and dropped;
--    course_template_versions becomes the one course_templates table.
DROP TABLE course_templates;
ALTER TABLE course_template_versions RENAME TO course_templates;
ALTER TABLE course_templates RENAME CONSTRAINT course_template_versions_pkey TO course_templates_pkey;

-- 3. Repoint offerings/sections/categories directly at course_templates (no version hop).
ALTER TABLE course_offerings RENAME COLUMN course_template_version_id TO course_template_id;
ALTER TABLE course_offerings
    RENAME CONSTRAINT course_offerings_course_template_version_id_fkey
        TO course_offerings_course_template_id_fkey;
ALTER INDEX idx_course_offerings_template_version RENAME TO idx_course_offerings_template_id;

ALTER TABLE course_sections RENAME COLUMN course_template_version_id TO course_template_id;
ALTER TABLE course_sections
    RENAME CONSTRAINT fk_course_sections_template_version TO fk_course_sections_template;
ALTER TABLE course_sections
    RENAME CONSTRAINT uk_course_sections_version_position TO uk_course_sections_template_position;
ALTER TABLE course_sections
    RENAME CONSTRAINT uk_course_sections_version_slug TO uk_course_sections_template_slug;
ALTER INDEX idx_course_sections_template_version_id RENAME TO idx_course_sections_template_id;

ALTER TABLE course_categories RENAME COLUMN course_template_version_id TO course_template_id;
ALTER TABLE course_categories
    RENAME CONSTRAINT fk_course_categories_template_version TO fk_course_categories_template;
