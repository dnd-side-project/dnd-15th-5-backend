
TRUNCATE TABLE report RESTART IDENTITY CASCADE;

INSERT INTO report (id, user_id, report_month, persona_type, score_exploration, score_town_expansion, score_daytime, score_impulsive, total_visit_count, new_town_count, new_place_count)
VALUES
    (1, 1, '2026-07-01', 'NIGHT_PILGRIM', 22.50, 15.00, 28.00, 34.00, 24, 5, 8),
    (2, 1, '2026-06-01', 'NEW_EXPLORER',  61.00, 40.00, 45.00, 30.00, 18, 3, 11),
    (3, 2, '2026-07-01', 'DAYTIME_SPENDER', 18.00, 20.00, 72.00, 15.00, 12, 2, 4);

INSERT INTO report_category_stat (report_id, category, percentage) VALUES
    (1, '카페', 60.00),
    (1, '먹거리', 25.00),
    (1, '놀거리', 15.00),
    (2, '카페', 35.00),
    (2, '먹거리', 40.00),
    (2, '놀거리', 25.00),
    (3, '카페', 20.00),
    (3, '먹거리', 55.00),
    (3, '놀거리', 25.00);

INSERT INTO report_town_rank (report_id, rank, town_name, visit_count) VALUES
    (1, 1, '성수동', 4),
    (1, 2, '연남동', 5),
    (1, 3, '방원동', 2),
    (2, 1, '홍대입구', 6),
    (2, 2, '연남동', 3),
    (3, 1, '역삼동', 7);

INSERT INTO report_place_rank (report_id, rank, place_id, place_name, visit_count) VALUES
    (1, 1, 101, '투썸플레이스 뚝섬지점', 9),
    (1, 2, 102, '블루보틀 성수', 6),
    (1, 3, 103, '밤삼킨 별', 3),
    (2, 1, 201, '스타벅스 홍대점', 5),
    (3, 1, 301, '한식당 역삼', 4);

INSERT INTO report_time_pattern (report_id, day_of_week, visit_hour, visit_count) VALUES
    (1, 1, 20, 2),
    (1, 2, 21, 3),
    (1, 3, 19, 3),
    (1, 4, 20, 2),
    (1, 5, 22, 8),
    (1, 6, 21, 4),
    (1, 7, 18, 2);

SELECT setval(pg_get_serial_sequence('report', 'id'), (SELECT MAX(id) FROM report));
