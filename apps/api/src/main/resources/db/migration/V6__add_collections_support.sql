-- =========================================
-- V6: Collections support + polymorphic order items
-- =========================================

-- =========================================================
-- COLLECTIONS
-- =========================================================
CREATE TABLE collections (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid (),
    title text NOT NULL,
    slug text NOT NULL UNIQUE,
    collection_type varchar(30) NOT NULL DEFAULT 'PACK',
    thumbnail_object_key text,
    short_description text,
    description text,
    price_bdt numeric(12, 2) NOT NULL DEFAULT 0,
    status varchar(30) NOT NULL DEFAULT 'DRAFT',
    published_at timestamptz,
    created_by uuid NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_collections_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT chk_collection_type CHECK (collection_type IN ('PACK', 'TRACK', 'DIPLOMA', 'DEGREE')),
    CONSTRAINT chk_collection_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    CONSTRAINT chk_collection_price CHECK (price_bdt >= 0)
);

CREATE INDEX idx_collections_status_published_at ON collections (status, published_at);
CREATE INDEX idx_collections_created_by ON collections (created_by);

CREATE TABLE collection_courses (
    collection_id uuid NOT NULL,
    course_id uuid NOT NULL,
    position integer NOT NULL,
    is_mandatory boolean NOT NULL DEFAULT TRUE,
    PRIMARY KEY (collection_id, course_id),
    CONSTRAINT fk_collection_courses_collection FOREIGN KEY (collection_id) REFERENCES collections (id) ON DELETE CASCADE,
    CONSTRAINT fk_collection_courses_course FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE CASCADE,
    CONSTRAINT uk_collection_courses_collection_position UNIQUE (collection_id, position),
    CONSTRAINT chk_collection_courses_position_positive CHECK (position > 0)
);

CREATE INDEX idx_collection_courses_course_id ON collection_courses (course_id);

CREATE TABLE collection_enrollments (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL,
    collection_id uuid NOT NULL,
    source_order_item_id uuid,
    status varchar(30) NOT NULL DEFAULT 'ACTIVE',
    enrolled_at timestamptz NOT NULL DEFAULT now(),
    revoked_at timestamptz,
    completed_at timestamptz,
    expires_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT fk_collection_enrollments_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,

    CONSTRAINT fk_collection_enrollments_collection
        FOREIGN KEY (collection_id) REFERENCES collections (id) ON DELETE RESTRICT,

    CONSTRAINT fk_collection_enrollments_source_order_item
        FOREIGN KEY (source_order_item_id) REFERENCES order_items (id) ON DELETE SET NULL,

    CONSTRAINT uk_collection_enrollments_user_collection
        UNIQUE (user_id, collection_id),

    CONSTRAINT chk_collection_enrollment_status
        CHECK (status IN ('ACTIVE', 'REFUNDED', 'REVOKED'))
);

CREATE INDEX idx_collection_enrollments_user_id ON collection_enrollments (user_id);
CREATE INDEX idx_collection_enrollments_collection_id ON collection_enrollments (collection_id);
CREATE INDEX idx_collection_enrollments_source_order_item_id ON collection_enrollments (source_order_item_id);
CREATE INDEX idx_collection_enrollments_status ON collection_enrollments (status);
CREATE INDEX idx_collection_enrollments_expires_at ON collection_enrollments (expires_at);

-- =========================================================
-- ENROLLMENTS provenance (course-level)
-- =========================================================
ALTER TABLE enrollments
    ADD COLUMN source_order_item_id uuid,
    ADD COLUMN source_collection_id uuid;

ALTER TABLE enrollments
    ADD CONSTRAINT fk_enrollments_source_order_item
        FOREIGN KEY (source_order_item_id) REFERENCES order_items (id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_enrollments_source_collection
        FOREIGN KEY (source_collection_id) REFERENCES collections (id) ON DELETE SET NULL;

CREATE INDEX idx_enrollments_source_order_item_id ON enrollments (source_order_item_id);
CREATE INDEX idx_enrollments_source_collection_id ON enrollments (source_collection_id);

-- =========================================================
-- ORDER ITEMS (course/collection polymorphic line item)
-- =========================================================
ALTER TABLE order_items
    ADD COLUMN collection_id uuid,
    ADD COLUMN item_type varchar(20) NOT NULL DEFAULT 'COURSE',
    ADD COLUMN title_snapshot text NOT NULL DEFAULT '';

ALTER TABLE order_items
    DROP CONSTRAINT IF EXISTS fk_order_items_course;

ALTER TABLE order_items
    ALTER COLUMN course_id DROP NOT NULL;

ALTER TABLE order_items
    ADD CONSTRAINT fk_order_items_course FOREIGN KEY (course_id) REFERENCES courses (id),
    ADD CONSTRAINT fk_order_items_collection FOREIGN KEY (collection_id) REFERENCES collections (id),
    ADD CONSTRAINT chk_order_items_type CHECK (item_type IN ('COURSE', 'COLLECTION')),
    ADD CONSTRAINT chk_order_items_course_or_collection CHECK (
        (item_type = 'COURSE' AND course_id IS NOT NULL AND collection_id IS NULL)
        OR (item_type = 'COLLECTION' AND course_id IS NULL AND collection_id IS NOT NULL)
    );

ALTER TABLE order_items
    DROP CONSTRAINT IF EXISTS uk_order_items_order_course;

CREATE UNIQUE INDEX uk_order_items_order_course_not_null
    ON order_items (order_id, course_id)
    WHERE course_id IS NOT NULL;

CREATE UNIQUE INDEX uk_order_items_order_collection_not_null
    ON order_items (order_id, collection_id)
    WHERE collection_id IS NOT NULL;

CREATE INDEX idx_order_items_collection_id ON order_items (collection_id);

-- Keep captured line-item pricing immutable after creation.
CREATE OR REPLACE FUNCTION enforce_order_item_pricing_immutable()
RETURNS trigger AS $$
BEGIN
    IF NEW.price_bdt IS DISTINCT FROM OLD.price_bdt THEN
        RAISE EXCEPTION 'order_items.price_bdt is immutable after insert';
    END IF;

    IF NEW.discount_bdt IS DISTINCT FROM OLD.discount_bdt THEN
        RAISE EXCEPTION 'order_items.discount_bdt is immutable after insert';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_order_items_pricing_immutable
BEFORE UPDATE ON order_items
FOR EACH ROW
EXECUTE FUNCTION enforce_order_item_pricing_immutable();
