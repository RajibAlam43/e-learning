ALTER TABLE course_templates
    ADD COLUMN youtube_video_id varchar(11),
    ADD CONSTRAINT chk_course_templates_youtube_video_id
        CHECK (youtube_video_id IS NULL OR youtube_video_id ~ '^[A-Za-z0-9_-]{11}$');
