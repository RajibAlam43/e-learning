-- A provider video is reusable across cloned template versions. Each lesson still owns exactly
-- one media_assets row (uk_media_lesson), while provider identifiers are indexed for lookup rather
-- than incorrectly treated as globally unique ownership keys.
DROP INDEX IF EXISTS uk_media_assets_provider_asset_id_not_null;
DROP INDEX IF EXISTS uk_media_assets_provider_playback_id_not_null;

CREATE INDEX idx_media_assets_provider_asset_id_not_null
    ON media_assets (provider, provider_asset_id)
    WHERE provider_asset_id IS NOT NULL;

CREATE INDEX idx_media_assets_provider_playback_id_not_null
    ON media_assets (provider, playback_id)
    WHERE playback_id IS NOT NULL;
