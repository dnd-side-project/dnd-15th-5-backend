CREATE TABLE report_summary_exposure (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT      NOT NULL REFERENCES users (id),
    exposure_date   DATE        NOT NULL,
    selected_type   VARCHAR(20) NOT NULL,
    created_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_report_summary_exposure_user_date
        UNIQUE (user_id, exposure_date)
);
CREATE INDEX idx_report_summary_exposure_user_id ON report_summary_exposure (user_id);
