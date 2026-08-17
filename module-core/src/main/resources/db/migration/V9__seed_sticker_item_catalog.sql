INSERT INTO sticker_item (category, name) VALUES
    ('카페',   '도넛'),
    ('카페',   '커피'),
    ('카페',   '아이스크림'),
    ('놀거리', '다트'),
    ('놀거리', '마이크'),
    ('놀거리', 'LP'),
    ('음식점', '감자튀김'),
    ('음식점', '피자'),
    ('음식점', '뒤집개')
ON CONFLICT DO NOTHING;
