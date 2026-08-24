-- Split reusable curriculum from concrete, enrollable course deliveries.
-- Existing public course UUIDs are retained as course_offering UUIDs.

ALTER TABLE courses RENAME TO course_templates;

ALTER TABLE course_templates
    ADD COLUMN internal_key varchar(150);

UPDATE course_templates SET internal_key = slug;

ALTER TABLE course_templates
    ALTER COLUMN internal_key SET NOT NULL,
    ADD CONSTRAINT uk_course_templates_internal_key UNIQUE (internal_key);

CREATE TABLE course_template_versions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    course_template_id uuid NOT NULL REFERENCES course_templates (id) ON DELETE CASCADE,
    version_number integer NOT NULL,
    status varchar(30) NOT NULL DEFAULT 'DRAFT',
    title text NOT NULL,
    title_en text,
    thumbnail_object_key text,
    short_description text,
    short_description_en text,
    description text,
    description_en text,
    highlights jsonb,
    highlights_en jsonb,
    course_outcomes jsonb,
    course_outcomes_en jsonb,
    requirements jsonb,
    requirements_en jsonb,
    prerequisites jsonb,
    prerequisites_en jsonb,
    level varchar(30) NOT NULL,
    language varchar(20) NOT NULL,
    live_session_count integer NOT NULL DEFAULT 0,
    quiz_count integer NOT NULL DEFAULT 0,
    recorded_hours_count integer NOT NULL DEFAULT 0,
    preview_lesson_id uuid REFERENCES lessons (id) ON DELETE SET NULL,
    estimated_duration_minutes integer,
    target_audience text,
    target_audience_en text,
    published_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uk_course_template_versions_template_version
        UNIQUE (course_template_id, version_number),
    CONSTRAINT chk_course_template_versions_version_positive CHECK (version_number > 0),
    CONSTRAINT chk_course_template_versions_status
        CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    CONSTRAINT chk_course_template_versions_level
        CHECK (level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')),
    CONSTRAINT chk_course_template_versions_language CHECK (language IN ('BN', 'EN')),
    CONSTRAINT chk_course_template_versions_counts_non_negative
        CHECK (live_session_count >= 0 AND quiz_count >= 0 AND recorded_hours_count >= 0),
    CONSTRAINT chk_course_template_versions_duration_non_negative
        CHECK (estimated_duration_minutes IS NULL OR estimated_duration_minutes >= 0)
);

INSERT INTO course_template_versions (
    course_template_id, version_number, status, title, title_en,
    thumbnail_object_key, short_description, short_description_en, description, description_en,
    highlights, highlights_en, course_outcomes, course_outcomes_en, requirements, requirements_en,
    prerequisites, prerequisites_en, level, language, live_session_count, quiz_count,
    recorded_hours_count, preview_lesson_id, estimated_duration_minutes, target_audience,
    target_audience_en, published_at, created_at, updated_at
)
SELECT id, 1, status, title, title_en,
       thumbnail_object_key, short_description, short_description_en, description, description_en,
       highlights, highlights_en, course_outcomes, course_outcomes_en, requirements, requirements_en,
       prerequisites, prerequisites_en, level, language, live_session_count, quiz_count,
       recorded_hours_count, preview_lesson_id, estimated_duration_minutes, target_audience,
       target_audience_en, published_at, created_at, updated_at
FROM course_templates;

CREATE TABLE course_offerings (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    course_template_version_id uuid NOT NULL
        REFERENCES course_template_versions (id) ON DELETE RESTRICT,
    slug text NOT NULL UNIQUE,
    name text,
    price_bdt numeric(12, 2) NOT NULL DEFAULT 0,
    study_mode varchar(30) NOT NULL DEFAULT 'COHORT_BASED',
    status varchar(30) NOT NULL DEFAULT 'DRAFT',
    published_at timestamptz,
    is_featured boolean NOT NULL DEFAULT false,
    featured_position integer,
    featured_at timestamptz,
    is_free boolean NOT NULL DEFAULT false,
    timezone varchar(80),
    enrollment_starts_at timestamptz,
    enrollment_ends_at timestamptz,
    starts_at timestamptz,
    ends_at timestamptz,
    capacity integer,
    access_duration_days integer,
    created_by uuid NOT NULL REFERENCES users (id),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT chk_course_offerings_study_mode
        CHECK (study_mode IN ('SELF_PACED', 'COHORT_BASED')),
    CONSTRAINT chk_course_offerings_status
        CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    CONSTRAINT chk_course_offerings_price CHECK (price_bdt >= 0),
    CONSTRAINT chk_course_offerings_capacity CHECK (capacity IS NULL OR capacity > 0),
    CONSTRAINT chk_course_offerings_access_duration
        CHECK (access_duration_days IS NULL OR access_duration_days > 0),
    CONSTRAINT chk_course_offerings_enrollment_window
        CHECK (enrollment_ends_at IS NULL OR enrollment_starts_at IS NULL
               OR enrollment_ends_at > enrollment_starts_at),
    CONSTRAINT chk_course_offerings_delivery_window
        CHECK (ends_at IS NULL OR starts_at IS NULL OR ends_at > starts_at),
    CONSTRAINT chk_course_offerings_featured_position
        CHECK (featured_position IS NULL OR featured_position > 0),
    CONSTRAINT chk_course_offerings_featured_fields
        CHECK ((is_featured AND featured_position IS NOT NULL AND featured_at IS NOT NULL)
               OR (NOT is_featured AND featured_position IS NULL AND featured_at IS NULL))
);

INSERT INTO course_offerings (
    id, course_template_version_id, slug, name, price_bdt, study_mode, status, published_at,
    is_featured, featured_position, featured_at, is_free, created_by, created_at, updated_at
)
SELECT ct.id, ctv.id, ct.slug, ct.title, ct.price_bdt,
       CASE WHEN ct.study_mode = 'SCHEDULED' THEN 'COHORT_BASED' ELSE ct.study_mode END,
       ct.status, ct.published_at, ct.is_featured, ct.featured_position, ct.featured_at,
       ct.is_free, ct.created_by, ct.created_at, ct.updated_at
FROM course_templates ct
JOIN course_template_versions ctv ON ctv.course_template_id = ct.id AND ctv.version_number = 1;

CREATE INDEX idx_course_offerings_status_published_at
    ON course_offerings (status, published_at);
CREATE INDEX idx_course_offerings_template_version
    ON course_offerings (course_template_version_id);
CREATE INDEX idx_course_offerings_created_by ON course_offerings (created_by);
CREATE INDEX idx_course_offerings_public_featured
    ON course_offerings (featured_position, featured_at DESC, id DESC)
    WHERE status = 'PUBLISHED' AND is_featured;

-- Delivery-owned relationships now point to concrete course offerings.
ALTER TABLE enrollments DROP CONSTRAINT fk_enrollments_course;
ALTER TABLE enrollments RENAME COLUMN course_id TO course_offering_id;
ALTER TABLE enrollments
    ADD CONSTRAINT fk_enrollments_course_offering
        FOREIGN KEY (course_offering_id) REFERENCES course_offerings (id) ON DELETE CASCADE;

ALTER TABLE course_instructors DROP CONSTRAINT fk_course_instructors_course;
ALTER TABLE course_instructors RENAME TO course_offering_instructors;
ALTER TABLE course_offering_instructors RENAME COLUMN course_id TO course_offering_id;
ALTER TABLE course_offering_instructors
    ADD CONSTRAINT fk_course_offering_instructors_offering
        FOREIGN KEY (course_offering_id) REFERENCES course_offerings (id) ON DELETE CASCADE;

ALTER TABLE collection_courses DROP CONSTRAINT fk_collection_courses_course;
ALTER TABLE collection_courses RENAME COLUMN course_id TO course_offering_id;
ALTER TABLE collection_courses
    ADD CONSTRAINT fk_collection_courses_offering
        FOREIGN KEY (course_offering_id) REFERENCES course_offerings (id) ON DELETE CASCADE;

ALTER TABLE order_items DROP CONSTRAINT fk_order_items_course;
ALTER TABLE order_items RENAME COLUMN course_id TO course_offering_id;
ALTER TABLE order_items
    ADD CONSTRAINT fk_order_items_course_offering
        FOREIGN KEY (course_offering_id) REFERENCES course_offerings (id);

ALTER TABLE certificates DROP CONSTRAINT fk_certificates_course;
ALTER TABLE certificates RENAME COLUMN course_id TO course_offering_id;
ALTER TABLE certificates
    ADD CONSTRAINT fk_certificates_course_offering
        FOREIGN KEY (course_offering_id) REFERENCES course_offerings (id) ON DELETE RESTRICT;

ALTER TABLE course_reviews DROP CONSTRAINT fk_course_reviews_course;
ALTER TABLE course_reviews RENAME COLUMN course_id TO course_offering_id;
ALTER TABLE course_reviews
    ADD CONSTRAINT fk_course_reviews_course_offering
        FOREIGN KEY (course_offering_id) REFERENCES course_offerings (id) ON DELETE CASCADE;

ALTER TABLE course_announcements
    DROP CONSTRAINT course_announcements_course_id_fkey;
ALTER TABLE course_announcements RENAME COLUMN course_id TO course_offering_id;
ALTER TABLE course_announcements
    ADD CONSTRAINT fk_course_announcements_course_offering
        FOREIGN KEY (course_offering_id) REFERENCES course_offerings (id) ON DELETE CASCADE;

-- Categories belong to the exact template version shown by an offering.
ALTER TABLE course_categories DROP CONSTRAINT fk_course_categories_course;
ALTER TABLE course_categories ADD COLUMN course_template_version_id uuid;
UPDATE course_categories cc
SET course_template_version_id = ctv.id
FROM course_template_versions ctv
WHERE ctv.course_template_id = cc.course_id AND ctv.version_number = 1;
ALTER TABLE course_categories DROP CONSTRAINT course_categories_pkey;
ALTER TABLE course_categories DROP COLUMN course_id;
ALTER TABLE course_categories
    ALTER COLUMN course_template_version_id SET NOT NULL,
    ADD CONSTRAINT course_categories_pkey
        PRIMARY KEY (course_template_version_id, category_id),
    ADD CONSTRAINT fk_course_categories_template_version
        FOREIGN KEY (course_template_version_id)
        REFERENCES course_template_versions (id) ON DELETE CASCADE;

-- Sections are the only direct curriculum children of a template version.
ALTER TABLE course_sections ADD COLUMN course_template_version_id uuid;
UPDATE course_sections cs
SET course_template_version_id = ctv.id
FROM course_template_versions ctv
WHERE ctv.course_template_id = cs.course_id AND ctv.version_number = 1;

ALTER TABLE live_classes
    DROP CONSTRAINT IF EXISTS fk_live_classes_section_course,
    DROP CONSTRAINT IF EXISTS fk_live_classes_course;
ALTER TABLE quizzes DROP CONSTRAINT IF EXISTS fk_quizzes_section_course;
ALTER TABLE quizzes DROP CONSTRAINT IF EXISTS fk_quizzes_course;
ALTER TABLE lessons
    DROP CONSTRAINT IF EXISTS fk_lessons_section_course,
    DROP CONSTRAINT IF EXISTS fk_lessons_course;
ALTER TABLE course_sections
    DROP CONSTRAINT IF EXISTS fk_course_sections_course,
    DROP CONSTRAINT IF EXISTS uk_course_sections_course_position,
    DROP CONSTRAINT IF EXISTS uk_course_sections_course_slug,
    DROP CONSTRAINT IF EXISTS uk_course_sections_id_course_id;

ALTER TABLE course_sections DROP COLUMN course_id;
ALTER TABLE course_sections
    ALTER COLUMN course_template_version_id SET NOT NULL,
    ADD CONSTRAINT fk_course_sections_template_version
        FOREIGN KEY (course_template_version_id)
        REFERENCES course_template_versions (id) ON DELETE CASCADE,
    ADD CONSTRAINT uk_course_sections_version_position
        UNIQUE (course_template_version_id, position),
    ADD CONSTRAINT uk_course_sections_version_slug
        UNIQUE (course_template_version_id, slug);

DROP INDEX IF EXISTS idx_course_sections_course_id;
CREATE INDEX idx_course_sections_template_version_id
    ON course_sections (course_template_version_id);

-- Lessons and quizzes derive their version through their section.
ALTER TABLE lessons
    DROP CONSTRAINT IF EXISTS uk_lessons_course_slug,
    DROP CONSTRAINT IF EXISTS uk_lessons_id_course_id,
    DROP CONSTRAINT IF EXISTS uk_lessons_id_section_course;
DROP INDEX IF EXISTS idx_lessons_course_id;
ALTER TABLE lessons DROP COLUMN course_id;
ALTER TABLE lessons
    ADD CONSTRAINT uk_lessons_section_slug UNIQUE (section_id, slug);

DROP INDEX IF EXISTS idx_quizzes_course_id;
ALTER TABLE quizzes DROP COLUMN course_id;

-- Turn the old scheduled curriculum items into reusable slots. The slot reuses the old
-- live-class UUID so existing section ordering remains stable.
CREATE TABLE live_class_slots (
    id uuid PRIMARY KEY,
    section_id uuid NOT NULL REFERENCES course_sections (id) ON DELETE CASCADE,
    title text NOT NULL,
    title_en text,
    description text,
    description_en text,
    expected_duration_minutes integer,
    is_mandatory boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT chk_live_class_slots_duration
        CHECK (expected_duration_minutes IS NULL OR expected_duration_minutes > 0)
);

INSERT INTO live_class_slots (
    id, section_id, title, title_en, description, description_en,
    expected_duration_minutes, is_mandatory, created_at, updated_at
)
SELECT id, section_id, title, title_en, description, description_en,
       GREATEST(1, CEIL(EXTRACT(EPOCH FROM (ends_at - starts_at)) / 60.0)::integer),
       true, created_at, updated_at
FROM live_classes;

ALTER TABLE section_items DROP CONSTRAINT chk_section_items_item_type;
ALTER TABLE section_items
    ADD CONSTRAINT chk_section_items_item_type
        CHECK (item_type IN ('LESSON', 'QUIZ', 'LIVE_CLASS'));

ALTER TABLE live_classes
    ADD COLUMN course_offering_id uuid,
    ADD COLUMN live_class_slot_id uuid,
    ADD COLUMN provisioning_mode varchar(30) NOT NULL DEFAULT 'API_PROVISIONED',
    ADD CONSTRAINT chk_live_classes_provisioning_mode
        CHECK (provisioning_mode IN ('API_PROVISIONED', 'EXTERNAL_URL'));
UPDATE live_classes
SET course_offering_id = course_id,
    live_class_slot_id = id;

DROP INDEX IF EXISTS idx_live_classes_course_id;
DROP INDEX IF EXISTS idx_live_classes_section_id;
DROP INDEX IF EXISTS idx_live_classes_instructor_id;
ALTER TABLE live_classes
    DROP COLUMN course_id,
    DROP COLUMN section_id,
    DROP COLUMN instructor_id,
    DROP COLUMN title,
    DROP COLUMN title_en,
    DROP COLUMN description,
    DROP COLUMN description_en,
    DROP COLUMN created_by,
    ALTER COLUMN course_offering_id SET NOT NULL,
    ALTER COLUMN live_class_slot_id SET NOT NULL,
    ADD CONSTRAINT fk_live_classes_course_offering
        FOREIGN KEY (course_offering_id) REFERENCES course_offerings (id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_live_classes_slot
        FOREIGN KEY (live_class_slot_id) REFERENCES live_class_slots (id) ON DELETE CASCADE,
    ADD CONSTRAINT uk_live_classes_offering_slot
        UNIQUE (course_offering_id, live_class_slot_id);

CREATE INDEX idx_live_classes_course_offering_id ON live_classes (course_offering_id);
CREATE INDEX idx_live_classes_slot_id ON live_classes (live_class_slot_id);
CREATE INDEX idx_live_class_slots_section_id ON live_class_slots (section_id);

-- Progress and assessment state belongs to a student's enrollment, not just a user/template item.
ALTER TABLE lesson_progress ADD COLUMN enrollment_id uuid;
UPDATE lesson_progress lp
SET enrollment_id = e.id
FROM lessons l
JOIN course_sections cs ON cs.id = l.section_id
JOIN course_offerings co ON co.course_template_version_id = cs.course_template_version_id
JOIN enrollments e ON e.course_offering_id = co.id
WHERE lp.lesson_id = l.id AND e.user_id = lp.user_id;
ALTER TABLE lesson_progress DROP CONSTRAINT lesson_progress_pkey;
ALTER TABLE lesson_progress DROP CONSTRAINT fk_lesson_progress_user;
ALTER TABLE lesson_progress DROP COLUMN user_id;
ALTER TABLE lesson_progress
    ALTER COLUMN enrollment_id SET NOT NULL,
    ADD CONSTRAINT lesson_progress_pkey PRIMARY KEY (enrollment_id, lesson_id),
    ADD CONSTRAINT fk_lesson_progress_enrollment
        FOREIGN KEY (enrollment_id) REFERENCES enrollments (id) ON DELETE CASCADE;
CREATE INDEX idx_lesson_progress_enrollment_id ON lesson_progress (enrollment_id);

ALTER TABLE quiz_attempts ADD COLUMN enrollment_id uuid;
UPDATE quiz_attempts qa
SET enrollment_id = e.id
FROM quizzes q
JOIN course_sections cs ON cs.id = q.section_id
JOIN course_offerings co ON co.course_template_version_id = cs.course_template_version_id
JOIN enrollments e ON e.course_offering_id = co.id
WHERE qa.quiz_id = q.id AND e.user_id = qa.user_id;
ALTER TABLE quiz_attempts DROP CONSTRAINT uk_quiz_attempts_quiz_user_attempt;
ALTER TABLE quiz_attempts
    ALTER COLUMN enrollment_id SET NOT NULL,
    ADD CONSTRAINT fk_quiz_attempts_enrollment
        FOREIGN KEY (enrollment_id) REFERENCES enrollments (id) ON DELETE CASCADE,
    ADD CONSTRAINT uk_quiz_attempts_quiz_enrollment_attempt
        UNIQUE (quiz_id, enrollment_id, attempt_no);
CREATE INDEX idx_quiz_attempts_enrollment_quiz ON quiz_attempts (enrollment_id, quiz_id);

ALTER TABLE live_class_attendance ADD COLUMN enrollment_id uuid;
UPDATE live_class_attendance lca
SET enrollment_id = e.id
FROM live_classes lc
JOIN enrollments e ON e.course_offering_id = lc.course_offering_id
WHERE lca.live_class_id = lc.id AND e.user_id = lca.user_id;
ALTER TABLE live_class_attendance
    ADD CONSTRAINT fk_live_class_attendance_enrollment
        FOREIGN KEY (enrollment_id) REFERENCES enrollments (id) ON DELETE SET NULL;
CREATE INDEX idx_live_class_attendance_enrollment_id
    ON live_class_attendance (enrollment_id);

ALTER TABLE course_reviews ADD COLUMN enrollment_id uuid;
UPDATE course_reviews cr
SET enrollment_id = e.id
FROM enrollments e
WHERE e.course_offering_id = cr.course_offering_id AND e.user_id = cr.user_id;
ALTER TABLE course_reviews
    ADD CONSTRAINT fk_course_reviews_enrollment
        FOREIGN KEY (enrollment_id) REFERENCES enrollments (id) ON DELETE CASCADE;

ALTER TABLE certificates ADD COLUMN enrollment_id uuid;
UPDATE certificates c
SET enrollment_id = e.id
FROM enrollments e
WHERE c.target_type = 'COURSE'
  AND e.course_offering_id = c.course_offering_id
  AND e.user_id = c.user_id;
ALTER TABLE certificates
    ADD CONSTRAINT fk_certificates_enrollment
        FOREIGN KEY (enrollment_id) REFERENCES enrollments (id) ON DELETE RESTRICT;

-- The stable template now contains identity only; all mutable content lives in versions.
ALTER TABLE course_templates
    DROP CONSTRAINT IF EXISTS fk_courses_preview_lesson,
    DROP CONSTRAINT IF EXISTS chk_course_language,
    DROP CONSTRAINT IF EXISTS chk_course_level,
    DROP CONSTRAINT IF EXISTS chk_course_study_mode,
    DROP CONSTRAINT IF EXISTS chk_course_status,
    DROP CONSTRAINT IF EXISTS chk_course_price,
    DROP CONSTRAINT IF EXISTS chk_courses_estimated_duration_non_negative,
    DROP CONSTRAINT IF EXISTS chk_courses_counts_non_negative,
    DROP CONSTRAINT IF EXISTS chk_courses_featured_position,
    DROP CONSTRAINT IF EXISTS chk_courses_featured_fields;

ALTER TABLE course_templates
    DROP COLUMN title,
    DROP COLUMN title_en,
    DROP COLUMN slug,
    DROP COLUMN thumbnail_object_key,
    DROP COLUMN short_description,
    DROP COLUMN short_description_en,
    DROP COLUMN description,
    DROP COLUMN description_en,
    DROP COLUMN highlights,
    DROP COLUMN highlights_en,
    DROP COLUMN price_bdt,
    DROP COLUMN course_outcomes,
    DROP COLUMN course_outcomes_en,
    DROP COLUMN requirements,
    DROP COLUMN requirements_en,
    DROP COLUMN prerequisites,
    DROP COLUMN prerequisites_en,
    DROP COLUMN level,
    DROP COLUMN language,
    DROP COLUMN study_mode,
    DROP COLUMN status,
    DROP COLUMN published_at,
    DROP COLUMN is_featured,
    DROP COLUMN featured_position,
    DROP COLUMN featured_at,
    DROP COLUMN is_free,
    DROP COLUMN live_session_count,
    DROP COLUMN quiz_count,
    DROP COLUMN recorded_hours_count,
    DROP COLUMN preview_lesson_id,
    DROP COLUMN estimated_duration_minutes,
    DROP COLUMN target_audience,
    DROP COLUMN target_audience_en,
    DROP COLUMN created_by;

DROP INDEX IF EXISTS idx_courses_status_published_at;
DROP INDEX IF EXISTS idx_courses_preview_lesson_id;
DROP INDEX IF EXISTS idx_courses_public_featured;

-- Snapshot the concrete offerings included in a collection order. Fulfilment, student views,
-- and certificates must not change when collection membership changes later.
CREATE TABLE order_item_courses (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    order_item_id uuid NOT NULL REFERENCES order_items (id) ON DELETE CASCADE,
    course_offering_id uuid NOT NULL REFERENCES course_offerings (id) ON DELETE RESTRICT,
    position integer NOT NULL,
    CONSTRAINT uk_order_item_courses_item_course UNIQUE (order_item_id, course_offering_id),
    CONSTRAINT chk_order_item_courses_position_positive CHECK (position > 0)
);

INSERT INTO order_item_courses (order_item_id, course_offering_id, position)
SELECT oi.id, cc.course_offering_id, cc.position
FROM order_items oi
JOIN collection_courses cc ON cc.collection_id = oi.collection_id
WHERE oi.item_type = 'COLLECTION';

CREATE INDEX idx_order_item_courses_order_item ON order_item_courses (order_item_id, position);
CREATE INDEX idx_order_item_courses_course ON order_item_courses (course_offering_id);

ALTER TABLE certificates
    ADD COLUMN revoked_by uuid REFERENCES users (id) ON DELETE SET NULL,
    ADD COLUMN revocation_reason text;
