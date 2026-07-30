ALTER TABLE lessons DROP COLUMN thumbnail_url;

ALTER TABLE collections
    ADD COLUMN title_en text,
    ADD COLUMN short_description_en text,
    ADD COLUMN description_en text;

ALTER TABLE courses
    ADD COLUMN title_en text,
    ADD COLUMN short_description_en text,
    ADD COLUMN description_en text,
    ADD COLUMN highlights_en jsonb,
    ADD COLUMN course_outcomes_en jsonb,
    ADD COLUMN requirements_en jsonb,
    ADD COLUMN prerequisites_en jsonb,
    ADD COLUMN target_audience_en text;

ALTER TABLE categories ADD COLUMN name_en varchar(150);

ALTER TABLE course_sections
    ADD COLUMN title_en text,
    ADD COLUMN description_en text;

ALTER TABLE lessons ADD COLUMN title_en text;
ALTER TABLE lesson_resources ADD COLUMN title_en text;
ALTER TABLE media_assets
    ADD COLUMN title_en text,
    ADD COLUMN thumbnail_object_key text;
ALTER TABLE quizzes ADD COLUMN title_en text;

ALTER TABLE quiz_questions
    ADD COLUMN question_text_en text,
    ADD COLUMN explanation_text_en text;

ALTER TABLE quiz_choices ADD COLUMN choice_text_en text;

ALTER TABLE live_classes
    ADD COLUMN title_en text,
    ADD COLUMN description_en text;

ALTER TABLE instructor_profiles
    ADD COLUMN headline_en text,
    ADD COLUMN institution_en text,
    ADD COLUMN expertise_area_en text,
    ADD COLUMN about_en text,
    ADD COLUMN credentials_text_en text,
    ADD COLUMN specialties_en jsonb;

ALTER TABLE faqs
    ADD COLUMN question_en text,
    ADD COLUMN answer_en text;

ALTER TABLE order_items ADD COLUMN title_snapshot_en text;

CREATE INDEX idx_live_class_registrants_live_class_id_status
    ON live_class_registrants (live_class_id, status);
