CREATE TABLE mux_video_uploads (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    lesson_id uuid NOT NULL,
    upload_id text NOT NULL,
    asset_id text,
    playback_id text,
    status varchar(30) NOT NULL,
    filename text NOT NULL,
    content_type varchar(100) NOT NULL,
    size_bytes bigint NOT NULL,
    expires_at timestamptz NOT NULL,
    error_message text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_mux_video_uploads_lesson
        FOREIGN KEY (lesson_id) REFERENCES lessons (id) ON DELETE CASCADE,
    CONSTRAINT uk_mux_video_upload_id UNIQUE (upload_id),
    CONSTRAINT chk_mux_video_upload_status
        CHECK (status IN ('WAITING', 'PROCESSING', 'READY', 'FAILED')),
    CONSTRAINT chk_mux_video_upload_size CHECK (size_bytes > 0)
);

CREATE INDEX idx_mux_video_uploads_lesson_created
    ON mux_video_uploads (lesson_id, created_at DESC);

CREATE UNIQUE INDEX uk_mux_video_upload_asset_id_not_null
    ON mux_video_uploads (asset_id)
    WHERE asset_id IS NOT NULL;

CREATE TABLE mux_webhook_events (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id text NOT NULL,
    event_type varchar(100) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uk_mux_webhook_event_id UNIQUE (event_id)
);
