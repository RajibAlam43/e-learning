CREATE TABLE live_class_recordings (
    id uuid PRIMARY KEY,
    live_class_id uuid NOT NULL REFERENCES live_classes(id),
    owner_id uuid NOT NULL REFERENCES users(id),
    method varchar(20) NOT NULL CHECK (method IN ('BROWSER', 'EXTENSION')),
    status varchar(20) NOT NULL CHECK (status IN ('UPLOADING', 'QUEUED', 'PROCESSING', 'READY', 'FAILED')),
    mime_type varchar(255) NOT NULL,
    object_prefix varchar(255) NOT NULL UNIQUE,
    upload_id varchar(255),
    upload_token_hash varchar(255),
    upload_expires_at timestamptz,
    size_bytes bigint,
    duration_seconds double precision,
    published_at timestamptz,
    processing_until timestamptz,
    processing_claim uuid,
    attempts integer NOT NULL DEFAULT 0,
    failure_message varchar(255),
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);
CREATE INDEX idx_recordings_class ON live_class_recordings(live_class_id, created_at DESC);
CREATE INDEX idx_recordings_processing ON live_class_recordings(status, processing_until)
    WHERE status IN ('QUEUED', 'PROCESSING');
