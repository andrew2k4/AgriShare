-- Local authentication (step 1 of the move off Firebase): accounts can now carry their own
-- password hash, a username, a verified e-mail and a status. firebase_uid becomes optional
-- and is kept only to migrate existing Firebase accounts.

ALTER TABLE app_users ALTER COLUMN firebase_uid DROP NOT NULL;

ALTER TABLE app_users ADD COLUMN username           VARCHAR(30);
ALTER TABLE app_users ADD COLUMN password_hash      VARCHAR(255);
ALTER TABLE app_users ADD COLUMN email_verified_at  TIMESTAMP;
ALTER TABLE app_users ADD COLUMN status             VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE app_users ADD COLUMN last_login_at      TIMESTAMP;
ALTER TABLE app_users ADD COLUMN failed_login_count INTEGER     NOT NULL DEFAULT 0;
ALTER TABLE app_users ADD COLUMN locked_until       TIMESTAMP;

-- E-mails and usernames are stored lower-cased by the application; the indexes on lower()
-- also protect against rows written by hand with a different case.
UPDATE app_users SET email = lower(trim(email)) WHERE email IS NOT NULL;
UPDATE app_users SET email = NULL WHERE email = '';

-- Refuse to continue if two accounts share an e-mail: they must be merged by hand (each may
-- own projects through owner_uid), never silently. The message lists the e-mails to fix.
DO $$
DECLARE
    duplicates TEXT;
BEGIN
    SELECT string_agg(email || ' (' || n || ' accounts)', ', ')
    INTO duplicates
    FROM (SELECT email, count(*) AS n FROM app_users
          WHERE email IS NOT NULL GROUP BY email HAVING count(*) > 1) d;
    IF duplicates IS NOT NULL THEN
        RAISE EXCEPTION 'V14: several app_users share the same e-mail, merge them before migrating: %', duplicates;
    END IF;
END $$;

CREATE UNIQUE INDEX uq_app_users_email    ON app_users (lower(email));
CREATE UNIQUE INDEX uq_app_users_username ON app_users (lower(username));

-- External identities (Google) linked to a local account.
CREATE TABLE user_identities (
    id         BIGSERIAL    PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES app_users (id) ON DELETE CASCADE,
    provider   VARCHAR(20)  NOT NULL,
    subject    VARCHAR(255) NOT NULL,
    email      VARCHAR(255),
    created_at TIMESTAMP    NOT NULL,
    CONSTRAINT uq_user_identities_provider_subject UNIQUE (provider, subject)
);
CREATE INDEX idx_user_identities_user ON user_identities (user_id);

-- Single-use tokens sent by e-mail (e-mail verification, password reset).
-- Only the SHA-256 of the token is stored, never the token itself.
CREATE TABLE user_tokens (
    id         BIGSERIAL   PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES app_users (id) ON DELETE CASCADE,
    type       VARCHAR(30) NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP   NOT NULL,
    used_at    TIMESTAMP,
    created_at TIMESTAMP   NOT NULL,
    CONSTRAINT uq_user_tokens_hash UNIQUE (token_hash)
);
CREATE INDEX idx_user_tokens_user_type ON user_tokens (user_id, type);
