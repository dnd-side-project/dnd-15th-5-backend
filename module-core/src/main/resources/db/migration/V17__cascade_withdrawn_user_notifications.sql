DELETE FROM notifications notification
WHERE NOT EXISTS (
    SELECT 1
    FROM users
    WHERE users.id = notification.user_id
);

ALTER TABLE notifications
    ADD CONSTRAINT fk_notifications_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE;
