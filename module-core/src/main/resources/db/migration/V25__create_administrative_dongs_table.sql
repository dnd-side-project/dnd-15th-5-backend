CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE administrative_dongs (
    code         VARCHAR(20)  NOT NULL PRIMARY KEY,
    sido_name    VARCHAR(50)  NOT NULL,
    sigungu_name VARCHAR(50)  NOT NULL,
    dong_name    VARCHAR(50)  NOT NULL,
    full_name    VARCHAR(150) NOT NULL,
    search_text  VARCHAR(160) NOT NULL
);

CREATE INDEX idx_administrative_dongs_search_text_trgm
    ON administrative_dongs USING gin (search_text gin_trgm_ops);
