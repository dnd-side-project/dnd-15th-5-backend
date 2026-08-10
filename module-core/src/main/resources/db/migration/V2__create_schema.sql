CREATE TABLE users (
    id                 BIGSERIAL PRIMARY KEY,
    nickname           VARCHAR(30)  NOT NULL,
    profile_image_url  VARCHAR(500),
    email              VARCHAR(320),
    status             VARCHAR(20)  NOT NULL,
    withdrawn_at       TIMESTAMP,
    created_at         TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE social_accounts (
    id                BIGSERIAL    PRIMARY KEY,
    provider          VARCHAR(20)  NOT NULL,
    provider_user_id  VARCHAR(255) NOT NULL,
    user_id           BIGINT       NOT NULL REFERENCES users (id),
    created_at        TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_social_accounts_provider_user_id
        UNIQUE (provider, provider_user_id)
);
CREATE INDEX idx_social_accounts_user_id ON social_accounts (user_id);

CREATE TABLE user_terms_agreements (
    id            BIGSERIAL   PRIMARY KEY,
    terms_type    VARCHAR(30) NOT NULL,
    terms_version VARCHAR(30) NOT NULL,
    agreed_at     TIMESTAMP NOT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_id       BIGINT      NOT NULL REFERENCES users (id)
);
CREATE INDEX idx_user_terms_agreements_user_id ON user_terms_agreements (user_id);


CREATE TABLE places (
    id                          BIGSERIAL PRIMARY KEY,
    google_place_id             TEXT,
    name                        VARCHAR(100) NOT NULL,
    road_address                VARCHAR(255) NOT NULL,
    administrative_dong_code    VARCHAR(20)  NOT NULL,
    administrative_dong_name    VARCHAR(100) NOT NULL,
    location                    GEOGRAPHY(POINT, 4326) NOT NULL,
    created_at                  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_places_google_place_id
        UNIQUE (google_place_id)
);

CREATE INDEX idx_places_location
    ON places
    USING GIST (location);


CREATE TABLE consumptions (
    id             BIGSERIAL    PRIMARY KEY,
    purchase_date  DATE         NOT NULL,
    purchase_time  TIME,
    amount         BIGINT       NOT NULL,
    category       VARCHAR(40)  NOT NULL,
    user_id        BIGINT       NOT NULL REFERENCES users (id),
    place_id       BIGINT       NOT NULL REFERENCES places (id),
    created_at     TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_consumptions_user_purchase_date ON consumptions (user_id, purchase_date);
CREATE INDEX idx_consumptions_place_id ON consumptions (place_id);

CREATE TABLE receipt_images (
    id               BIGSERIAL     PRIMARY KEY,
    object_key       VARCHAR(1024) NOT NULL,
    content_type     VARCHAR(100)  NOT NULL,
    file_size_bytes  BIGINT        NOT NULL,
    status           VARCHAR(20)   NOT NULL,
    expires_at       TIMESTAMP,
    attached_at      TIMESTAMP,
    created_at       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_id          BIGINT        NOT NULL REFERENCES users (id),
    consumption_id   BIGINT REFERENCES consumptions (id)    -- 등록 전에는 NULL
);
CREATE INDEX idx_receipt_images_status_expires_at ON receipt_images (status, expires_at);
CREATE INDEX idx_receipt_images_consumption_id ON receipt_images (consumption_id);

CREATE TABLE report (
    id                   BIGSERIAL   PRIMARY KEY,
    user_id              BIGINT      NOT NULL REFERENCES users (id),
    report_month         DATE        NOT NULL,
    persona_type         VARCHAR(50) NOT NULL,
    score_exploration    NUMERIC(5,2),
    score_town_expansion NUMERIC(5,2),
    score_daytime        NUMERIC(5,2),
    score_impulsive      NUMERIC(5,2),
    total_visit_count    INT         NOT NULL DEFAULT 0,
    new_town_count       INT         NOT NULL DEFAULT 0,
    new_place_count      INT         NOT NULL DEFAULT 0,
    created_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_report_user_month
        UNIQUE (user_id, report_month)
);

CREATE TABLE report_category_stat (
    id         BIGSERIAL    PRIMARY KEY,
    report_id  BIGINT       NOT NULL REFERENCES report (id) ON DELETE CASCADE,
    category   VARCHAR(50)  NOT NULL,
    percentage NUMERIC(5,2) NOT NULL
);

CREATE TABLE report_town_rank (
    id           BIGSERIAL    PRIMARY KEY,
    report_id    BIGINT       NOT NULL REFERENCES report (id) ON DELETE CASCADE,
    rank         INT          NOT NULL,
    town_name    VARCHAR(100) NOT NULL,
    visit_count  INT          NOT NULL
);

CREATE TABLE report_place_rank (
    id           BIGSERIAL    PRIMARY KEY,
    report_id    BIGINT       NOT NULL REFERENCES report (id) ON DELETE CASCADE,
    rank         INT          NOT NULL,
    place_id     BIGINT       NOT NULL REFERENCES places (id),
    place_name   VARCHAR(150) NOT NULL,
    visit_count  INT          NOT NULL
);

CREATE TABLE report_time_pattern (
    id          BIGSERIAL PRIMARY KEY,
    report_id   BIGINT    NOT NULL REFERENCES report (id) ON DELETE CASCADE,
    day_of_week SMALLINT  NOT NULL, -- 1=월요일 ~ 7=일요일
    visit_hour  SMALLINT  NOT NULL, -- 0~23시
    visit_count INT       NOT NULL
);
