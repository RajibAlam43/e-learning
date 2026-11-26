CREATE TABLE google_meet_connections (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    google_subject varchar(255) NOT NULL,
    email varchar(255) NOT NULL,
    encrypted_refresh_token text NOT NULL,
    granted_scopes text NOT NULL,
    refresh_expires_at timestamptz,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);
CREATE TABLE google_meet_oauth_states (
    id uuid PRIMARY KEY,
    state_hash varchar(255) NOT NULL UNIQUE,
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    browser_hash varchar(255) NOT NULL,
    nonce_hash varchar(255) NOT NULL,
    encrypted_verifier text NOT NULL,
    requested_scopes text NOT NULL,
    expires_at timestamptz NOT NULL,
    consumed_at timestamptz,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);
CREATE INDEX idx_google_meet_oauth_expiry ON google_meet_oauth_states(expires_at);
