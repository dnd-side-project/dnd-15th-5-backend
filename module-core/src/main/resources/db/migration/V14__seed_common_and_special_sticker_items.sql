INSERT INTO sticker_item (category, name) VALUES
    ('공통',   '눈'),
    ('공통',   '따봉'),
    ('스페셜', '왕관')
ON CONFLICT (category, name) DO NOTHING;
