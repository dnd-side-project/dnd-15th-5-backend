-- 이름만 바꾸고 ID는 유지해 기존 소비기록과의 연결을 보존한다.
-- 쇼핑카트를 장바구니로 바꿀 때 이름이 겹치지 않도록, 기존 장바구니를 먼저 세탁으로 바꾼다.

UPDATE sticker_item
SET name = '세탁'
WHERE category = '편의점/마트' AND name = '장바구니';

UPDATE sticker_item AS sticker
SET name = correction.new_name
FROM (VALUES
    ('카페', '말차', '녹차'),
    ('음식점', '국수', '라면'),
    ('음식점', '파스타', '스파게티'),
    ('음식점', '식기', '접시'),
    ('음식점', '파에야', '빠에야'),
    ('편의점/마트', '쇼핑카트', '장바구니'),
    ('편의점/마트', '음료', '사과 주스'),
    ('취미/놀거리', '게임', '게임기'),
    ('쇼핑', '원피스', '드레스'),
    ('쇼핑', '안경', '선글라스'),
    ('운동', '요가', '명상'),
    ('운동', '축구', '축구공'),
    ('운동', '테니스', '테니스공'),
    ('미용/뷰티', '헤어', '헤어컷'),
    ('미용/뷰티', '네일', '매니큐어'),
    ('기타', '위치핀', '압정')
) AS correction(category, old_name, new_name)
WHERE sticker.category = correction.category
  AND sticker.name = correction.old_name;
