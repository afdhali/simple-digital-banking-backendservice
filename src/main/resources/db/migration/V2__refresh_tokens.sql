-- Refresh tokens are opaque random strings. Only their SHA-256 hash is stored, so a database
-- leak does not leak usable tokens. Tokens belong to a "family" (= one login session): every refresh
-- rotates the token inside the same family, and replaying an already-used token revokes the family.
CREATE TABLE refresh_tokens (
    id                 UUID        PRIMARY KEY,
    user_id            UUID        NOT NULL REFERENCES users (id),
    family_id          UUID        NOT NULL,
    token_hash         VARCHAR(64) NOT NULL,
    session_started_at TIMESTAMPTZ NOT NULL,
    expires_at         TIMESTAMPTZ NOT NULL,
    revoked_at         TIMESTAMPTZ,
    created_at         TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_refresh_tokens_hash UNIQUE (token_hash)
);

CREATE INDEX idx_refresh_tokens_family  ON refresh_tokens (family_id);
CREATE INDEX idx_refresh_tokens_user    ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_expires ON refresh_tokens (expires_at);
