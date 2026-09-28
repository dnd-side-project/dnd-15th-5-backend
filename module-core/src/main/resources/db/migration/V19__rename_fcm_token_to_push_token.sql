ALTER TABLE users RENAME COLUMN fcm_token TO push_token;
ALTER TABLE users RENAME COLUMN fcm_token_updated_at TO push_token_updated_at;

ALTER INDEX ux_users_fcm_token RENAME TO ux_users_push_token;

ALTER TABLE notifications RENAME COLUMN fcm_message_id TO push_ticket_id;
