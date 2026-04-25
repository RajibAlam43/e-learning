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