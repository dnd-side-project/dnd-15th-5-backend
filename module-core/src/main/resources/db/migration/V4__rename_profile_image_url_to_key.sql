ALTER TABLE users
    RENAME COLUMN profile_image_url TO profile_image_key;

ALTER TABLE users
    ALTER COLUMN profile_image_key TYPE VARCHAR(1024);
