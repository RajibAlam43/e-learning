ALTER TABLE order_item_courses
    ADD COLUMN is_mandatory boolean NOT NULL DEFAULT true;

-- V12 snapshots predate this flag. Recover it from the matching collection row when that row still
-- exists; otherwise retain the safe default that the purchased course is required.
UPDATE order_item_courses snapshot
SET is_mandatory = collection_course.is_mandatory
FROM order_items item, collection_courses collection_course
WHERE item.id = snapshot.order_item_id
  AND collection_course.collection_id = item.collection_id
  AND collection_course.course_offering_id = snapshot.course_offering_id;

-- These flags previously defaulted false but were not consulted by completion logic, so legacy
-- curriculum was effectively mandatory. Preserve that behavior when the flags become functional;
-- authors can explicitly mark content optional after this migration.
UPDATE course_sections SET is_mandatory = true;
UPDATE lessons SET is_mandatory = true;

ALTER TABLE course_sections ALTER COLUMN is_mandatory SET DEFAULT true;
ALTER TABLE lessons ALTER COLUMN is_mandatory SET DEFAULT true;
