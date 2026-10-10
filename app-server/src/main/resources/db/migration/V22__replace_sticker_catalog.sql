-- 공통·왕관 자동 지급 로직 제거와 함께 적용

-- 1. 카테고리 8개, 스티커 64개
-- category_order = 1: 기존 기록을 옮길 카테고리별 대표 스티커
CREATE TEMP TABLE new_sticker_catalog (
    category VARCHAR(40) NOT NULL,
    name VARCHAR(50) NOT NULL,
    category_order INTEGER NOT NULL,
    PRIMARY KEY (category, name),
    UNIQUE (category, category_order)
) ON COMMIT DROP;

INSERT INTO new_sticker_catalog (category, name, category_order) VALUES
    ('카페', '커피', 1),
    ('카페', '케이크', 2),
    ('카페', '크루아상', 3),
    ('카페', '아이스크림', 4),
    ('카페', '컵케이크', 5),
    ('카페', '쿠키', 6),
    ('카페', '버블티', 7),
    ('카페', '말차', 8),
    ('음식점', '밥', 1),
    ('음식점', '국수', 2),
    ('음식점', '파스타', 3),
    ('음식점', '피자', 4),
    ('음식점', '초밥', 5),
    ('음식점', '식기', 6),
    ('음식점', '햄버거', 7),
    ('음식점', '파에야', 8),
    ('운동', '근육', 1),
    ('운동', '달리기', 2),
    ('운동', '역도', 3),
    ('운동', '요가', 4),
    ('운동', '자전거', 5),
    ('운동', '운동화', 6),
    ('운동', '테니스', 7),
    ('운동', '축구', 8),
    ('미용/뷰티', '립스틱', 1),
    ('미용/뷰티', '네일', 2),
    ('미용/뷰티', '마사지', 3),
    ('미용/뷰티', '거울', 4),
    ('미용/뷰티', '로션', 5),
    ('미용/뷰티', '헤어커트', 6),
    ('미용/뷰티', '반짝임', 7),
    ('미용/뷰티', '이발소', 8),
    ('편의점/마트', '쇼핑카트', 1),
    ('편의점/마트', '장바구니', 2),
    ('편의점/마트', '편의점', 3),
    ('편의점/마트', '통조림', 4),
    ('편의점/마트', '음료', 5),
    ('편의점/마트', '우유', 6),
    ('편의점/마트', '물', 7),
    ('편의점/마트', '사과', 8),
    ('쇼핑', '쇼핑백', 1),
    ('쇼핑', '선물', 2),
    ('쇼핑', '원피스', 3),
    ('쇼핑', '하이힐', 4),
    ('쇼핑', '핸드백', 5),
    ('쇼핑', '반지', 6),
    ('쇼핑', '청바지', 7),
    ('쇼핑', '안경', 8),
    ('취미/놀거리', '팔레트', 1),
    ('취미/놀거리', '영화', 2),
    ('취미/놀거리', '게임', 3),
    ('취미/놀거리', '마이크', 4),
    ('취미/놀거리', '볼링', 5),
    ('취미/놀거리', '관람차', 6),
    ('취미/놀거리', '헤드폰', 7),
    ('취미/놀거리', '카메라', 8),
    ('기타', '신용카드', 1),
    ('기타', '강아지', 2),
    ('기타', '고양이', 3),
    ('기타', '책', 4),
    ('기타', '알약', 5),
    ('기타', '열쇠', 6),
    ('기타', '자동차', 7),
    ('기타', '우체국', 8);

-- 2. 새 스티커 추가. 카테고리·이름이 같으면 기존 ID 유지
INSERT INTO sticker_item (category, name)
SELECT category, name
FROM new_sticker_catalog
ORDER BY category, category_order
ON CONFLICT (category, name) DO NOTHING;

-- 3. 없앨 스티커를 소비기록 카테고리의 대표 스티커로 교체
-- 예: 카페 기록의 왕관 → 커피, 운동 기록의 눈 → 근육
UPDATE consumptions c
SET sticker_item_id = replacement.id
FROM sticker_item old_sticker,
     new_sticker_catalog representative,
     sticker_item replacement
WHERE c.sticker_item_id = old_sticker.id
  AND representative.category = c.category
  AND representative.category_order = 1
  AND replacement.category = representative.category
  AND replacement.name = representative.name
  AND NOT EXISTS (
      SELECT 1
      FROM new_sticker_catalog catalog
      WHERE catalog.category = old_sticker.category
        AND catalog.name = old_sticker.name
  );

-- 4. 새 목록에서 빠진 스티커 삭제
-- 교체하지 못한 기록이 남으면 FK 제약으로 전체 롤백
DELETE FROM sticker_item old_sticker
WHERE NOT EXISTS (
    SELECT 1
    FROM new_sticker_catalog catalog
    WHERE catalog.category = old_sticker.category
      AND catalog.name = old_sticker.name
);
