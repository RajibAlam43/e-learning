ALTER TABLE users
    ALTER COLUMN password_hash DROP NOT NULL;

ALTER TABLE users
    ADD COLUMN google_subject varchar(255);

ALTER TABLE users
    ADD CONSTRAINT chk_users_authentication_method
        CHECK (
            (password_hash IS NOT NULL AND btrim(password_hash) <> '')
            OR (google_subject IS NOT NULL AND btrim(google_subject) <> '')
        );

CREATE UNIQUE INDEX uk_users_google_subject_not_null
    ON users (google_subject)
    WHERE google_subject IS NOT NULL;
