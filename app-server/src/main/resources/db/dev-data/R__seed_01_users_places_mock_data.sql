-- Repeatable migration :파일 내용이 바뀔 때마다 Flyway가 다시 실행
-- consumptions/report 등이 FK로 참조하므로 가장 먼저 실행돼야 함 (파일명 접두 01)
TRUNCATE TABLE users RESTART IDENTITY CASCADE;
TRUNCATE TABLE places RESTART IDENTITY CASCADE;

INSERT INTO users (id, nickname, status) VALUES
    (1, '수민', 'ACTIVE'),
    (2, '지호', 'ACTIVE'),
    (3, '하은', 'ACTIVE');
SELECT setval(pg_get_serial_sequence('users', 'id'), (SELECT MAX(id) FROM users));

INSERT INTO places (id, name, road_address, administrative_dong_code, administrative_dong_name, location) VALUES
    (101, '투썸플레이스 뚝섬지점',   '서울 성동구 성수이로 100', '1120510100', '성수동',   ST_SetSRID(ST_MakePoint(127.0557, 37.5447), 4326)),
    (102, '블루보틀 성수',           '서울 성동구 아차산로 200', '1120510100', '성수동',   ST_SetSRID(ST_MakePoint(127.0568, 37.5426), 4326)),
    (103, '밤삼킨 별',               '서울 마포구 연남로 10',   '1144012400', '연남동',   ST_SetSRID(ST_MakePoint(126.9257, 37.5636), 4326)),
    (104, '연남동 신규 브런치집',    '서울 마포구 연남로 20',   '1144012400', '연남동',   ST_SetSRID(ST_MakePoint(126.9251, 37.5629), 4326)),
    (105, '방원동 편의점',           '서울 마포구 방원길 5',    '1144012500', '방원동',   ST_SetSRID(ST_MakePoint(126.9203, 37.5602), 4326)),
    (106, '성수동 베이글집',         '서울 성동구 성수이로 120', '1120510100', '성수동',   ST_SetSRID(ST_MakePoint(127.0561, 37.5452), 4326)),
    (201, '스타벅스 홍대점',         '서울 마포구 양화로 158',  '1144010700', '홍대입구', ST_SetSRID(ST_MakePoint(126.9236, 37.5568), 4326)),
    (301, '한식당 역삼',             '서울 강남구 역삼로 300',  '1168010100', '역삼동',   ST_SetSRID(ST_MakePoint(127.0367, 37.5006), 4326)),
    (302, '역삼동 카페거리',         '서울 강남구 역삼로 310',  '1168010100', '역삼동',   ST_SetSRID(ST_MakePoint(127.0372, 37.5011), 4326)),
    (303, '신논현 헬스장 매점',      '서울 강남구 강남대로 400', '1168010200', '신논현동', ST_SetSRID(ST_MakePoint(127.0248, 37.5045), 4326)),
    (401, '신촌동 브런치카페',       '서울 서대문구 신촌로 50', '1141012200', '신촌동',   ST_SetSRID(ST_MakePoint(126.9368, 37.5596), 4326)),
    (402, '신촌동 분식집',           '서울 서대문구 신촌로 60', '1141012200', '신촌동',   ST_SetSRID(ST_MakePoint(126.9372, 37.5601), 4326)),
    (403, '신촌동 편의점',           '서울 서대문구 신촌로 70', '1141012200', '신촌동',   ST_SetSRID(ST_MakePoint(126.9375, 37.5605), 4326));
SELECT setval(pg_get_serial_sequence('places', 'id'), (SELECT MAX(id) FROM places));
