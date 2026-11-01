CREATE TABLE user_towns (
    id                       BIGSERIAL PRIMARY KEY,
    user_id                  BIGINT           NOT NULL REFERENCES users (id),
    administrative_dong_code VARCHAR(20)      NOT NULL,
    administrative_dong_name VARCHAR(100)     NOT NULL,
    latitude                 DOUBLE PRECISION NOT NULL,
    longitude                DOUBLE PRECISION NOT NULL,
    selected_at              TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at               TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_user_towns_user_dong
        UNIQUE (user_id, administrative_dong_code)
);
CREATE INDEX idx_user_towns_user_id ON user_towns (user_id);
