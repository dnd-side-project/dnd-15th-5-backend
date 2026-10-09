ALTER TABLE users
    ALTER COLUMN nickname TYPE VARCHAR(16),
    ADD COLUMN profile_image_code VARCHAR(20) NOT NULL DEFAULT 'BLUE',
    ADD CONSTRAINT ck_users_profile_image_code
        CHECK (profile_image_code IN ('BLUE', 'SKY_BLUE', 'PINK', 'YELLOW', 'TEAL', 'RED'));
