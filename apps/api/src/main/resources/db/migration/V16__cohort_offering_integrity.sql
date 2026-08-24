ALTER TABLE live_classes
    ADD COLUMN title_override text,
    ADD COLUMN title_en_override text,
    ADD COLUMN description_override text,
    ADD COLUMN description_en_override text;

CREATE TABLE payment_attempts (
    id uuid PRIMARY KEY,
    order_id uuid NOT NULL,
    provider varchar(50) NOT NULL,
    provider_txn_id varchar(255) NOT NULL,
    redirect_url text NOT NULL,
    expires_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_payment_attempts_order
        FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT uk_payment_attempts_provider_txn
        UNIQUE (provider, provider_txn_id)
);

CREATE INDEX idx_payment_attempts_order_provider_created
    ON payment_attempts(order_id, provider, created_at DESC);

-- SectionItem is the canonical mixed-curriculum order. These legacy constraints made
-- ordinary swaps fail before SectionItem could be updated atomically.
WITH missing_items AS (
    SELECT l.section_id, 'LESSON'::varchar AS item_type, l.id AS item_id, l.position AS legacy_position
    FROM lessons l
    WHERE NOT EXISTS (
        SELECT 1 FROM section_items si
        WHERE si.item_type = 'LESSON' AND si.item_id = l.id
    )
    UNION ALL
    SELECT q.section_id, 'QUIZ'::varchar AS item_type, q.id AS item_id, q.position AS legacy_position
    FROM quizzes q
    WHERE NOT EXISTS (
        SELECT 1 FROM section_items si
        WHERE si.item_type = 'QUIZ' AND si.item_id = q.id
    )
), ranked_items AS (
    SELECT missing_items.*,
           COALESCE(
               (SELECT MAX(si.position)
                FROM section_items si
                WHERE si.section_id = missing_items.section_id),
               0
           ) + ROW_NUMBER() OVER (
               PARTITION BY missing_items.section_id
               ORDER BY missing_items.legacy_position, missing_items.item_type, missing_items.item_id
           ) AS canonical_position
    FROM missing_items
)
INSERT INTO section_items (id, section_id, item_type, item_id, position, created_at, updated_at)
SELECT gen_random_uuid(), section_id, item_type, item_id, canonical_position, now(), now()
FROM ranked_items;

ALTER TABLE lessons DROP CONSTRAINT IF EXISTS uk_lessons_section_position;
DROP INDEX IF EXISTS uk_quizzes_section_position;
