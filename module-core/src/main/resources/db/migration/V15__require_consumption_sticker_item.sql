UPDATE consumptions
SET sticker_item_id = (
    SELECT id
    FROM sticker_item
    WHERE category = '공통' AND name = '눈'
)
WHERE sticker_item_id IS NULL;

ALTER TABLE consumptions
    ALTER COLUMN sticker_item_id SET NOT NULL;
