CREATE INDEX idx_report_persona_type ON report (persona_type);
CREATE INDEX idx_report_place_rank_report_id ON report_place_rank (report_id);
CREATE INDEX idx_report_place_rank_place_id ON report_place_rank (place_id);
CREATE INDEX idx_report_category_stat_report_id ON report_category_stat (report_id);
CREATE INDEX idx_report_category_stat_category ON report_category_stat (category);

ALTER TABLE report_place_rank RENAME COLUMN first_visited_month TO first_visited_date;
