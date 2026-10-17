-- 기존 스티커 ID와 소비기록 연결을 유지하면서 디자인과 다른 이름만 수정한다.
UPDATE sticker_item AS sticker
SET name = correction.new_name
FROM (VALUES
    ('운동', '운동화', '수영'),
    ('미용/뷰티', '이발소', '사우나'),
    ('미용/뷰티', '헤어커트', '헤어'),
    ('편의점/마트', '물', '휴지'),
    ('쇼핑', '하이힐', '운동화'),
    ('기타', '열쇠', '위치핀'),
    ('기타', '우체국', '병원')
) AS correction(category, old_name, new_name)
WHERE sticker.category = correction.category
  AND sticker.name = correction.old_name;
