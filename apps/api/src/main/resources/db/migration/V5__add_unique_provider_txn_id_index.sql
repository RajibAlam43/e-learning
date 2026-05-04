CREATE UNIQUE INDEX IF NOT EXISTS uk_orders_provider_provider_txn_id
    ON orders (provider, provider_txn_id)
    WHERE provider_txn_id IS NOT NULL;
