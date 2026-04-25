ALTER TABLE enrollments
    ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT now();

ALTER TABLE media_assets DROP CONSTRAINT chk_media_provider;
ALTER TABLE media_assets
    ADD CONSTRAINT chk_media_provider
        CHECK (provider IN ('mux', 'youtube', 'r2'));

ALTER TABLE media_assets DROP CONSTRAINT chk_media_asset_type;
ALTER TABLE media_assets
    ADD CONSTRAINT chk_media_asset_type
        CHECK (asset_type IN ('video', 'pdf', 'image'));

ALTER TABLE orders DROP CONSTRAINT chk_order_provider;
ALTER TABLE orders
    ADD CONSTRAINT chk_order_provider
        CHECK (provider IN ('sslcommerz'));

-- 1 quiz per lesson (for lesson-bound quizzes)
CREATE UNIQUE INDEX uk_quizzes_lesson_id_not_null
    ON quizzes (lesson_id)
    WHERE lesson_id IS NOT NULL;

-- At most 1 course-level final quiz (lesson_id is null)
CREATE UNIQUE INDEX uk_quizzes_course_id_when_lesson_null
    ON quizzes (course_id)
    WHERE lesson_id IS NULL;

-- Safer composite uniqueness: provider-specific transaction id
CREATE UNIQUE INDEX uk_orders_provider_provider_txn_id_not_null
    ON orders (provider, provider_txn_id)
    WHERE provider_txn_id IS NOT NULL;

-- Safer composite uniqueness: provider-specific event id
CREATE UNIQUE INDEX uk_payment_events_provider_provider_event_id_not_null
    ON payment_events (provider, provider_event_id)
    WHERE provider_event_id IS NOT NULL;

-- Provider-scoped playback id uniqueness
CREATE UNIQUE INDEX uk_media_assets_provider_playback_id_not_null
    ON media_assets (provider, playback_id)
    WHERE playback_id IS NOT NULL;
