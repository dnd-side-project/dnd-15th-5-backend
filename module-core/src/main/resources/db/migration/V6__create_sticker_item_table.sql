CREATE TABLE sticker_item (
    id       BIGSERIAL   PRIMARY KEY,
    category VARCHAR(40) NOT NULL,
    name     VARCHAR(50) NOT NULL,

    CONSTRAINT uk_sticker_item_category_name
        UNIQUE (category, name)
);

ALTER TABLE consumptions
    ADD COLUMN sticker_item_id BIGINT REFERENCES sticker_item (id);

CREATE INDEX idx_consumptions_sticker_item_id ON consumptions (sticker_item_id);
