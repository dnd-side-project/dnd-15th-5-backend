ALTER TABLE users
    ADD COLUMN fcm_token            VARCHAR(512),
    ADD COLUMN push_enabled         BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN fcm_token_updated_at TIMESTAMP;

CREATE UNIQUE INDEX ux_users_fcm_token ON users (fcm_token) WHERE fcm_token IS NOT NULL;

CREATE TABLE notifications (
                               id              BIGSERIAL PRIMARY KEY,
                               user_id         BIGINT NOT NULL,
                               type            VARCHAR(30) NOT NULL,
                               title           VARCHAR(100) NOT NULL,
                               body            VARCHAR(500) NOT NULL,
                               is_read         BOOLEAN NOT NULL DEFAULT FALSE,
                               read_at         TIMESTAMP,
                               push_status     VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                               fcm_message_id  VARCHAR(255),
                               created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX ix_notifications_user_id_id ON notifications (user_id, id DESC);
CREATE INDEX ix_notifications_user_id_is_read ON notifications (user_id, is_read);