ALTER TABLE consumptions
    DROP CONSTRAINT consumptions_user_id_fkey,
    ADD CONSTRAINT fk_consumptions_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE;

ALTER TABLE receipt_images
    DROP CONSTRAINT receipt_images_user_id_fkey,
    ADD CONSTRAINT fk_receipt_images_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE;

ALTER TABLE report
    DROP CONSTRAINT report_user_id_fkey,
    ADD CONSTRAINT fk_report_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE;

ALTER TABLE place_likes
    DROP CONSTRAINT place_likes_user_id_fkey,
    ADD CONSTRAINT fk_place_likes_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE;
