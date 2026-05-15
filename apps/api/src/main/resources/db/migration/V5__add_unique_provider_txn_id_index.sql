CREATE UNIQUE INDEX IF NOT EXISTS uk_orders_provider_provider_txn_id
    ON orders (provider, provider_txn_id)
    WHERE provider_txn_id IS NOT NULL;

ALTER TABLE live_classes
    DROP CONSTRAINT IF EXISTS fk_live_classes_lesson_section_course;

DROP INDEX IF EXISTS idx_live_classes_lesson_id;

ALTER TABLE live_classes
    DROP COLUMN IF EXISTS lesson_id,
    DROP COLUMN IF EXISTS zoom_meeting_id,
    DROP COLUMN IF EXISTS zoom_start_url,
    DROP COLUMN IF EXISTS zoom_join_url;

ALTER TABLE live_class_registrants
    ADD COLUMN IF NOT EXISTS provider_registrant_id text,
    ADD COLUMN IF NOT EXISTS participant_join_url text;

ALTER TABLE live_class_registrants
    DROP COLUMN IF EXISTS zoom_registrant_id,
    DROP COLUMN IF EXISTS zoom_join_url;

ALTER TABLE live_class_attendance
    ADD COLUMN IF NOT EXISTS provider_participant_id text;

ALTER TABLE live_class_attendance
    DROP COLUMN IF EXISTS zoom_participant_id;

ALTER TABLE courses RENAME COLUMN thumbnail_url TO thumbnail_object_key;
