-- 다른 시드보다 먼저 실행돼야 함— consumptions.sticker_item_id가 이 테이블을 참조하므로
TRUNCATE TABLE sticker_item RESTART IDENTITY CASCADE;

INSERT INTO sticker_item (category, name) VALUES
    ('카페',   '도넛'),
    ('카페',   '커피'),
    ('카페',   '아이스크림'),
    ('놀거리', '다트'),
    ('놀거리', '마이크'),
    ('놀거리', 'LP'),
    ('음식점', '감자튀김'),
    ('음식점', '피자'),
    ('음식점', '뒤집개'),
    ('공통',   '눈'),
    ('공통',   '따봉'),
    ('스페셜', '왕관');
