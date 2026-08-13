UPDATE categories
SET name_en = name
WHERE name_en IS NULL OR btrim(name_en) = '';

ALTER TABLE categories
    ALTER COLUMN name_en SET NOT NULL,
    ADD CONSTRAINT chk_categories_name_not_blank CHECK (btrim(name) <> ''),
    ADD CONSTRAINT chk_categories_name_en_not_blank CHECK (btrim(name_en) <> '');
