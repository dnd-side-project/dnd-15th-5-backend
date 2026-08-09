ALTER TABLE users
    ALTER COLUMN nickname TYPE VARCHAR(10),
    ALTER COLUMN status SET DEFAULT 'PENDING_TERMS';

ALTER TABLE users
    ADD CONSTRAINT ck_users_status
        CHECK (status IN ('PENDING_TERMS', 'ACTIVE', 'SUSPENDED', 'WITHDRAWN'));

ALTER TABLE social_accounts
    RENAME CONSTRAINT uk_social_accounts_provider_user_id
        TO uk_social_accounts_provider_user;

ALTER TABLE social_accounts
    DROP CONSTRAINT social_accounts_user_id_fkey,
    ADD CONSTRAINT fk_social_accounts_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE,
    ADD CONSTRAINT uk_social_accounts_user_provider
        UNIQUE (user_id, provider),
    ADD CONSTRAINT ck_social_accounts_provider
        CHECK (provider IN ('GOOGLE', 'KAKAO'));

DROP INDEX idx_social_accounts_user_id;

ALTER TABLE user_terms_agreements
    DROP CONSTRAINT user_terms_agreements_user_id_fkey,
    ADD CONSTRAINT fk_user_terms_agreements_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE,
    ADD CONSTRAINT uk_user_terms_agreements
        UNIQUE (user_id, terms_type, terms_version);

DROP INDEX idx_user_terms_agreements_user_id;
