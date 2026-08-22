-- Add live classes to the canonical mixed ordering within a course section.
ALTER TABLE section_items
    DROP CONSTRAINT IF EXISTS chk_section_items_item_type;

ALTER TABLE section_items
    ADD CONSTRAINT chk_section_items_item_type
    CHECK (item_type IN ('LESSON', 'QUIZ', 'LIVE_CLASS'));

-- Existing live classes are appended after the current final item in their section,
-- ordered deterministically by schedule and id.
WITH ranked_live_classes AS (
    SELECT lc.id,
           lc.section_id,
           COALESCE(
               (SELECT MAX(si.position)
                FROM section_items si
                WHERE si.section_id = lc.section_id),
               0
           ) + ROW_NUMBER() OVER (
               PARTITION BY lc.section_id
               ORDER BY lc.starts_at, lc.id
           ) AS position
    FROM live_classes lc
)
INSERT INTO section_items (section_id, item_type, item_id, position)
SELECT section_id, 'LIVE_CLASS', id, position
FROM ranked_live_classes
ON CONFLICT (item_type, item_id) DO NOTHING;
