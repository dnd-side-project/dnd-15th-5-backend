CREATE INDEX idx_consumptions_user_place_recent
    ON consumptions (user_id, place_id, purchase_date DESC, purchase_time DESC, id DESC);
