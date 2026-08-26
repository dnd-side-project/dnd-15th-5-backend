ALTER TABLE report
    ADD COLUMN share_token VARCHAR(36);

CREATE UNIQUE INDEX uk_report_share_token ON report (share_token) WHERE share_token IS NOT NULL;
