-- 1. 소비기록에 사용할 업로드 이미지 정보
CREATE TABLE consumption_images (
    id               BIGSERIAL     PRIMARY KEY,
    user_id          BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    object_key       VARCHAR(1024) NOT NULL,
    content_type     VARCHAR(100)  NOT NULL,
    file_size_bytes  BIGINT        NOT NULL,
    status           VARCHAR(20)   NOT NULL,
    expires_at       TIMESTAMP,
    attached_at      TIMESTAMP,
    created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_consumption_images_object_key UNIQUE (object_key),
    CONSTRAINT ck_consumption_images_status
        CHECK (status IN ('TEMPORARY', 'ATTACHED', 'DELETING')),
    CONSTRAINT ck_consumption_images_file_size
        CHECK (file_size_bytes > 0)
);

CREATE INDEX idx_consumption_images_user_id ON consumption_images (user_id);
CREATE INDEX idx_consumption_images_status_expires_at ON consumption_images (status, expires_at);

-- 2. 금액 선택값 처리, 메모·사진 추가, 스티커와 사진 중 하나만 저장
ALTER TABLE consumptions
    ALTER COLUMN amount DROP NOT NULL,
    ALTER COLUMN sticker_item_id DROP NOT NULL,
    ADD COLUMN memo TEXT,
    ADD COLUMN image_id BIGINT REFERENCES consumption_images (id),
    ADD CONSTRAINT uk_consumptions_image_id UNIQUE (image_id),
    ADD CONSTRAINT ck_consumptions_sticker_or_image
        CHECK ((sticker_item_id IS NOT NULL) <> (image_id IS NOT NULL));
