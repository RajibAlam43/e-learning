-- =========================================
-- V2: Phone OTP verification + progress versioning
-- =========================================
-- 1) Verification codes (phone OTP use-cases)
CREATE TABLE verification_codes (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid (),
    user_id uuid NOT NULL,
    token_hash text NOT NULL UNIQUE,
    expires_at timestamptz NOT NULL,
    used_at timestamptz,
    revoked_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    purpose varchar(40) NOT NULL,
    phone_hash text NOT NULL,
    attempt_count integer NOT NULL DEFAULT 0,
    max_attempts integer NOT NULL DEFAULT 5,
    sent_count integer NOT NULL DEFAULT 1,
    last_sent_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_verification_codes_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_verification_codes_purpose CHECK (purpose IN ('VERIFY_PHONE', 'PASSWORD_RESET')),
    CONSTRAINT chk_verification_codes_attempt_count_non_negative CHECK (attempt_count >= 0),
    CONSTRAINT chk_verification_codes_max_attempts_positive CHECK (max_attempts > 0),
    CONSTRAINT chk_verification_codes_sent_count_positive CHECK (sent_count > 0),
    CONSTRAINT chk_verification_codes_used_or_revoked CHECK (used_at IS NULL OR revoked_at IS NULL)
);

CREATE INDEX idx_verification_codes_user ON verification_codes (user_id);

CREATE INDEX idx_verification_codes_purpose ON verification_codes (purpose);

CREATE INDEX idx_verification_codes_expires_at ON verification_codes (expires_at);

CREATE INDEX idx_verification_codes_user_purpose_active ON verification_codes (user_id, purpose)
WHERE
    used_at IS NULL AND revoked_at IS NULL;

-- 2) lesson_progress alignment with entity (versioned, completed_at based)
-- Add optimistic lock version column
ALTER TABLE lesson_progress
    ADD COLUMN version bigint NOT NULL DEFAULT 0;

-- Preserve semantic info before removing legacy completed boolean
UPDATE
    lesson_progress
SET
    completed_at = COALESCE(completed_at, updated_at)
WHERE
    completed = TRUE
    AND completed_at IS NULL;

-- Drop legacy boolean flag now that entity tracks completion by completed_at
ALTER TABLE lesson_progress
    DROP COLUMN completed;

-- 3) Change is_pulished column to status column
-- Drop old column
ALTER TABLE quizzes
    DROP COLUMN is_published;

-- Add new column
ALTER TABLE quizzes
    ADD COLUMN status varchar(30) NOT NULL DEFAULT 'DRAFT';

-- Add constraint
ALTER TABLE quizzes
    ADD CONSTRAINT chk_quizzes_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED'));
