ALTER TABLE faculty_members
    ADD COLUMN credentials_text text,
    ADD COLUMN credentials_text_en text,
    ADD COLUMN institution varchar(255),
    ADD COLUMN institution_en varchar(255),
    ADD COLUMN expertise_area varchar(255),
    ADD COLUMN expertise_area_en varchar(255),
    ADD COLUMN specialties jsonb,
    ADD COLUMN specialties_en jsonb,
    ADD COLUMN years_experience integer CHECK (years_experience BETWEEN 0 AND 100);
