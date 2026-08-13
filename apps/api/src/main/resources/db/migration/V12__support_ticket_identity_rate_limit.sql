ALTER TABLE support_tickets
    ADD COLUMN rate_limit_key_hash varchar(64),
    ADD CONSTRAINT chk_support_ticket_rate_limit_hash
        CHECK (rate_limit_key_hash IS NULL OR rate_limit_key_hash ~ '^[0-9a-f]{64}$');

CREATE INDEX idx_support_tickets_rate_limit_key_created_at
    ON support_tickets (rate_limit_key_hash, created_at DESC)
    WHERE rate_limit_key_hash IS NOT NULL;
