ALTER TABLE users
    ADD COLUMN consumption_reminder_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN monthly_report_enabled BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE users
SET consumption_reminder_enabled = push_enabled,
    monthly_report_enabled = push_enabled;

ALTER TABLE users DROP COLUMN push_enabled;
