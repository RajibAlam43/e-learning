-- =========================================
-- V3: Align schema with branch entity model
-- =========================================

-- =========================================================
-- 1) verification_codes: channel-aware + security metadata
-- =========================================================
ALTER TABLE verification_codes
    RENAME COLUMN phone_hash TO channel_hash;

ALTER TABLE verification_codes
    ADD COLUMN channel varchar(20),
    ADD COLUMN requested_from_ip varchar(45),
    ADD COLUMN user_agent varchar(500),
    ADD COLUMN security_metadata jsonb NOT NULL DEFAULT '{}'::jsonb;

ALTER TABLE verification_codes
    ALTER COLUMN channel SET DEFAULT 'PHONE',
    ALTER COLUMN channel SET NOT NULL,
    ALTER COLUMN max_attempts SET DEFAULT 3,
    ALTER COLUMN channel_hash SET NOT NULL;

-- Replace old purpose check (VERIFY_PHONE/PASSWORD_RESET) with current enum values.
ALTER TABLE verification_codes
    DROP CONSTRAINT IF EXISTS chk_verification_codes_purpose;

ALTER TABLE verification_codes
    ADD CONSTRAINT chk_verification_codes_purpose
    CHECK (purpose IN ('EMAIL_VERIFICATION', 'PHONE_VERIFICATION', 'PASSWORD_RESET'));

ALTER TABLE verification_codes
    ADD CONSTRAINT chk_verification_codes_channel
    CHECK (channel IN ('EMAIL', 'PHONE'));

CREATE INDEX IF NOT EXISTS idx_verification_codes_channel
    ON verification_codes (channel);

CREATE INDEX IF NOT EXISTS idx_verification_codes_channel_hash
    ON verification_codes (channel_hash);

CREATE INDEX IF NOT EXISTS idx_verification_codes_user_purpose_channel_active
    ON verification_codes (user_id, purpose, channel)
    WHERE used_at IS NULL AND revoked_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_verification_codes_user_channel_created_at
    ON verification_codes (user_id, channel, created_at);

CREATE INDEX IF NOT EXISTS idx_verification_codes_channel_hash_created_at
    ON verification_codes (channel_hash, created_at);

-- =========================================================
-- 2) courses.prerequisites: text -> jsonb list of strings
-- =========================================================
ALTER TABLE courses
    ALTER COLUMN prerequisites TYPE jsonb
    USING '[]'::jsonb;

-- =========================================================
-- 3) live_classes: provider-agnostic meeting fields
-- =========================================================
ALTER TABLE live_classes
    ADD COLUMN provider varchar(30),
    ADD COLUMN provider_meeting_id text,
    ADD COLUMN host_start_url text,
    ADD COLUMN participant_join_url text,
    ADD COLUMN provider_metadata jsonb,
    ADD COLUMN max_capacity integer;

ALTER TABLE live_classes
    ALTER COLUMN provider SET DEFAULT 'ZOOM',
    ALTER COLUMN provider SET NOT NULL;

ALTER TABLE live_classes
    ADD CONSTRAINT chk_live_class_provider
    CHECK (provider IN ('ZOOM', 'GOOGLE_MEET', 'TEAMS', 'OTHER'));

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_session_id_revoked_at
    ON refresh_tokens (session_id, revoked_at);
