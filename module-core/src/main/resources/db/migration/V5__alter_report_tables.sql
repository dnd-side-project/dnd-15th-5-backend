ALTER TABLE report
    ADD COLUMN new_sticker_count INT NOT NULL DEFAULT 0;

ALTER TABLE report_place_rank
    ADD COLUMN first_visited_month DATE;
