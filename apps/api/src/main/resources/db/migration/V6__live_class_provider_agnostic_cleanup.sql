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

UPDATE live_class_registrants
SET provider_registrant_id = COALESCE(provider_registrant_id, zoom_registrant_id),
    participant_join_url = COALESCE(participant_join_url, zoom_join_url);

ALTER TABLE live_class_registrants
    DROP COLUMN IF EXISTS zoom_registrant_id,
    DROP COLUMN IF EXISTS zoom_join_url;

ALTER TABLE live_class_attendance
    ADD COLUMN IF NOT EXISTS provider_participant_id text;

UPDATE live_class_attendance
SET provider_participant_id = COALESCE(provider_participant_id, zoom_participant_id);

ALTER TABLE live_class_attendance
    DROP COLUMN IF EXISTS zoom_participant_id;
