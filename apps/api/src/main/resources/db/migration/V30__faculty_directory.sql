CREATE TABLE faculty_groups (
    id uuid PRIMARY KEY,
    name varchar(200) NOT NULL,
    name_en varchar(200),
    position integer NOT NULL CHECK (position > 0),
    visible boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE faculty_members (
    id uuid PRIMARY KEY,
    group_id uuid NOT NULL REFERENCES faculty_groups(id) ON DELETE RESTRICT,
    instructor_id uuid REFERENCES instructor_profiles(user_id) ON DELETE CASCADE,
    name varchar(200),
    title varchar(300),
    title_en varchar(300),
    bio text,
    bio_en text,
    photo_object_key varchar(500),
    position integer NOT NULL CHECK (position > 0),
    homepage_position integer NOT NULL CHECK (homepage_position > 0),
    show_on_faculty boolean NOT NULL DEFAULT false,
    show_on_homepage boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CHECK (instructor_id IS NOT NULL OR name IS NOT NULL)
);
CREATE INDEX idx_faculty_members_group ON faculty_members(group_id);
CREATE UNIQUE INDEX uk_faculty_group_instructor ON faculty_members(group_id, instructor_id)
    WHERE instructor_id IS NOT NULL;
INSERT INTO faculty_groups(id, name, name_en, position, visible) VALUES
 ('30100000-0000-4000-8000-000000000001', 'শিক্ষকবৃন্দ', 'Teachers', 1, true),
 ('30100000-0000-4000-8000-000000000002', 'কর্মীবৃন্দ', 'Staff', 2, true),
 ('30100000-0000-4000-8000-000000000003', 'পরিচালনা পর্ষদ', 'Board of Directors', 3, true),
 ('30100000-0000-4000-8000-000000000004', 'উপদেষ্টা পর্ষদ', 'Board of Advisors', 4, true);
