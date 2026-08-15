CREATE TABLE place_likes (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT    NOT NULL REFERENCES users (id),
    place_id   BIGINT    NOT NULL REFERENCES places (id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_place_likes_user_place
        UNIQUE (user_id, place_id)
);
CREATE INDEX idx_place_likes_user_id  ON place_likes (user_id);
CREATE INDEX idx_place_likes_place_id ON place_likes (place_id);
